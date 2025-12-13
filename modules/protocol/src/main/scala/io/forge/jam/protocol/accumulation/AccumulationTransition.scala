package io.forge.jam.protocol.accumulation

import io.forge.jam.core.{ChainConfig, JamBytes, Hashing}
import io.forge.jam.core.primitives.Hash
import io.forge.jam.core.types.workpackage.WorkReport
import org.bouncycastle.jcajce.provider.digest.Keccak

import scala.collection.mutable
import java.nio.{ByteBuffer, ByteOrder}

/**
 * Accumulation State Transition Function.
 *
 * - Ready queue management and dependency resolution
 * - Work report partitioning (immediate vs queued)
 * - PVM execution orchestration
 * - Commitment root computation
 * - Statistics tracking
 */
object AccumulationTransition:

  /**
   * Execute the Accumulation STF.
   *
   * @param input The accumulation input containing slot and reports
   * @param preState The pre-transition state
   * @param config The accumulation configuration
   * @return Tuple of (post-transition state, output)
   */
  def stf(
    input: AccumulationInput,
    preState: AccumulationState,
    config: ChainConfig
  ): (AccumulationState, AccumulationOutput) =
    val m = (input.slot % config.epochLength).toInt
    val deltaT = Math.max((input.slot - preState.slot).toInt, 1)

    // 1. Collect all historically accumulated hashes (for dependency checking)
    val historicallyAccumulated = mutable.Set.from(preState.accumulated.flatten)

    // 2. Partition new reports into immediate vs queued
    val (immediateReports, queuedReports) = input.reports.partition { report =>
      report.context.prerequisites.isEmpty && report.segmentRootLookup.isEmpty
    }

    // 3. Track newly accumulated package hashes this block
    val newAccumulated = mutable.Set.empty[JamBytes]

    // Add immediate reports to accumulated set
    immediateReports.foreach { report =>
      val hash = JamBytes(report.packageSpec.hash.bytes.toArray)
      newAccumulated += hash
      historicallyAccumulated += hash
    }

    // 4. Build working copy of ready queue with edited dependencies
    val workingReadyQueue = preState.readyQueue.indices.map { slotIdx =>
      val oldRecords = preState.readyQueue(slotIdx)
      editReadyQueueRecords(oldRecords, historicallyAccumulated.toSet)
    }.toList.to(mutable.ListBuffer)

    // Add new queued reports to the current slot m (BEFORE extraction)
    val newRecords = queuedReports.map { report =>
      val prereqs = report.context.prerequisites.map(h => JamBytes(h.bytes.toArray))
      val segmentDeps = report.segmentRootLookup.map(l => JamBytes(l.workPackageHash.bytes.toArray))
      val allDeps = (prereqs ++ segmentDeps).filter(!historicallyAccumulated.contains(_))
      AccumulationReadyRecord(report, allDeps)
    }
    workingReadyQueue(m) = workingReadyQueue(m) ++ newRecords

    // 5. Extract accumulatable reports from ready queue
    val allQueuedWithSlots = workingReadyQueue.zipWithIndex.flatMap {
      case (records, slotIdx) =>
        records.map(record => (slotIdx, record))
    }.toList

    val (readyToAccumulate, stillQueuedWithSlots) = extractAccumulatableWithSlots(
      allQueuedWithSlots,
      historicallyAccumulated.toSet
    )

    // 6. Add ready-to-accumulate reports to accumulated set
    readyToAccumulate.foreach { report =>
      val hash = JamBytes(report.packageSpec.hash.bytes.toArray)
      newAccumulated += hash
      historicallyAccumulated += hash
    }

    // 7. Rebuild ready queue with remaining records
    val newQueuedReportsNotAccumulated = stillQueuedWithSlots
      .filter {
        case (slot, record) =>
          slot == m && newRecords.exists(nr =>
            JamBytes(nr.report.packageSpec.hash.bytes.toArray) ==
              JamBytes(record.report.packageSpec.hash.bytes.toArray)
          )
      }
      .map(_._2)

    val finalReadyQueue = (0 until config.epochLength).map { idx =>
      val i = ((m - idx) % config.epochLength + config.epochLength) % config.epochLength
      if i == 0 then
        // Current slot: ONLY new queued reports from this block
        newQueuedReportsNotAccumulated.toList
      else if i >= 1 && i < deltaT then
        // Slots that wrapped around - clear them
        List.empty[AccumulationReadyRecord]
      else
        // Other slots: keep remaining items that weren't accumulated
        stillQueuedWithSlots.filter(_._1 == idx).map(_._2).toList
    }.toList

    // 8. Execute PVM for accumulated reports (respecting gas budget)
    val allToAccumulate = immediateReports ++ readyToAccumulate
    val partialState = preState.toPartialState()

    // Calculate total gas budget
    val sumPrivilegedGas = partialState.alwaysAccers.values.sum
    val minTotalGas = config.maxAccumulationGas * config.coresCount + sumPrivilegedGas
    val totalGasLimit = Math.max(config.maxBlockGas, minTotalGas)

    // Create executor (now directly uses PVM, no strategy pattern needed)
    val executor = new AccumulationExecutor(config)

    // Execute outer accumulation with recursive deferred transfer processing
    val outerResult = outerAccumulate(
      partialState = partialState,
      transfers = List.empty,
      workReports = allToAccumulate,
      alwaysAccers = partialState.alwaysAccers.toMap,
      gasLimit = totalGasLimit,
      timeslot = input.slot,
      entropy = preState.entropy,
      executor = executor,
      config = config
    )

    // Determine which reports were actually accumulated (based on reportsAccumulated count)
    val reportsToAccumulate = allToAccumulate.take(outerResult.reportsAccumulated)

    // Rebuild actuallyAccumulated to only include reports that will actually be accumulated
    val actuallyAccumulated = mutable.Set.empty[JamBytes]
    reportsToAccumulate.foreach(report => actuallyAccumulated += JamBytes(report.packageSpec.hash.bytes.toArray))

    val newPartialState = outerResult.postState
    val gasUsedPerService = outerResult.gasUsedMap
    val commitments = outerResult.commitments

    // 9. Rotate accumulated array (sliding window)
    val newAccumulatedList = actuallyAccumulated.toList.sortBy(_.toHex)
    val newAccumulatedArray = (0 until config.epochLength).map { idx =>
      if idx == config.epochLength - 1 then
        // New items at last position
        newAccumulatedList
      else
        // Shift left by 1
        preState.accumulated.lift(idx + 1).getOrElse(List.empty)
    }.toList

    // 10. Update statistics
    val workItemsPerService = countWorkItemsPerService(reportsToAccumulate)
    val newStatistics = updateStatistics(
      preState.statistics,
      gasUsedPerService,
      workItemsPerService
    )

    // 11. Build accumulation stats for fresh service statistics computation
    val accumulationStats: Map[Long, (Long, Int)] = gasUsedPerService
      .map {
        case (serviceId, gasUsed) =>
          val count = workItemsPerService.getOrElse(serviceId, 0)
          serviceId -> (gasUsed, count)
      }
      .filter { case (_, (gas, count)) => gas > 0 || count > 0 }

    // 12. Update lastAccumulationSlot for all services in accumulationStats
    for (serviceId, _) <- accumulationStats do
      newPartialState.accounts.get(serviceId).foreach { account =>
        newPartialState.accounts(serviceId) = account.copy(
          info = account.info.copy(lastAccumulationSlot = input.slot)
        )
      }

    // 13. Build final state with R function for privilege merging
    val origManager = preState.privileges.bless
    val origDelegator = preState.privileges.designate
    val origRegistrar = preState.privileges.register
    val origAssigners = preState.privileges.assign

    // Get privilege snapshots for R function
    val privilegeSnapshots = outerResult.privilegeSnapshots
    val managerSnapshot = privilegeSnapshots.get(origManager)

    // Manager service controls itself - no R needed for manager
    val finalManager = newPartialState.manager

    // Apply R function for delegator
    val managerPostDelegator = managerSnapshot.map(_.delegator).getOrElse(origDelegator)
    val delegatorSnapshot = privilegeSnapshots.get(origDelegator)
    val delegatorPostDelegator = delegatorSnapshot.map(_.delegator).getOrElse(origDelegator)
    val finalDelegator = privilegeR(origDelegator, managerPostDelegator, delegatorPostDelegator)

    // Apply R function for registrar
    val managerPostRegistrar = managerSnapshot.map(_.registrar).getOrElse(origRegistrar)
    val registrarSnapshot = privilegeSnapshots.get(origRegistrar)
    val registrarPostRegistrar = registrarSnapshot.map(_.registrar).getOrElse(origRegistrar)
    val finalRegistrar = privilegeR(origRegistrar, managerPostRegistrar, registrarPostRegistrar)

    // Apply R function for each assigner
    val finalAssigners = origAssigners.zipWithIndex.map {
      case (origAssigner, c) =>
        val managerPostAssigner = managerSnapshot.flatMap(_.assigners.lift(c)).getOrElse(origAssigner)
        val assignerSnapshot = privilegeSnapshots.get(origAssigner)
        val assignerPostAssigner = assignerSnapshot.flatMap(_.assigners.lift(c)).getOrElse(origAssigner)
        privilegeR(origAssigner, managerPostAssigner, assignerPostAssigner)
    }

    // AlwaysAccers comes from manager's post-state
    val finalAlwaysAccers = newPartialState.alwaysAccers

    val finalState = AccumulationState(
      slot = input.slot,
      entropy = JamBytes(preState.entropy.toArray),
      readyQueue = finalReadyQueue,
      accumulated = newAccumulatedArray,
      privileges = Privileges(
        bless = finalManager,
        assign = finalAssigners,
        designate = finalDelegator,
        register = finalRegistrar,
        alwaysAcc = finalAlwaysAccers.toList.sortBy(_._1).map {
          case (id, gas) =>
            AlwaysAccItem(id, gas)
        }
      ),
      statistics = newStatistics,
      accounts = newPartialState.toAccumulationServiceItems(),
      rawServiceDataByStateKey = newPartialState.rawServiceDataByStateKey,
      rawServiceAccountsByStateKey = newPartialState.rawServiceAccountsByStateKey
    )

    // 14. Compute commitment root from yields
    val outputHash = computeCommitmentRoot(commitments)

    (finalState, AccumulationOutput(outputHash))

  /**
   * Merging privilege updates.
   */
  private def privilegeR(original: Long, managerPost: Long, holderPost: Long): Long =
    if managerPost == original then holderPost else managerPost

  /**
   * Edit ready queue records by removing accumulated reports and pruning dependencies.
   */
  private def editReadyQueueRecords(
    records: List[AccumulationReadyRecord],
    accumulatedHashes: Set[JamBytes]
  ): List[AccumulationReadyRecord] =
    records
      .filter { record =>
        val reportHash = JamBytes(record.report.packageSpec.hash.bytes.toArray)
        !accumulatedHashes.contains(reportHash)
      }
      .map { record =>
        AccumulationReadyRecord(
          report = record.report,
          dependencies = record.dependencies.filter(!accumulatedHashes.contains(_))
        )
      }

  /**
   * Extract accumulatable reports while preserving slot information.
   */
  private def extractAccumulatableWithSlots(
    queueWithSlots: List[(Int, AccumulationReadyRecord)],
    initiallyAccumulated: Set[JamBytes]
  ): (List[WorkReport], List[(Int, AccumulationReadyRecord)]) =
    val accumulated = mutable.Set.from(initiallyAccumulated)
    val result = mutable.ListBuffer.empty[WorkReport]
    var remaining = queueWithSlots

    var continue = true
    while continue do
      val (ready, notReady) = remaining.partition {
        case (_, record) =>
          record.dependencies.forall(accumulated.contains)
      }
      if ready.isEmpty then
        continue = false
      else
        ready.foreach {
          case (_, record) =>
            result += record.report
            accumulated += JamBytes(record.report.packageSpec.hash.bytes.toArray)
        }
        remaining = notReady

    (result.toList, remaining)

  /**
   * Result of outer accumulation.
   */
  case class OuterAccumulationResult(
    reportsAccumulated: Int,
    postState: PartialState,
    gasUsedMap: Map[Long, Long],
    commitments: Set[Commitment],
    privilegeSnapshots: Map[Long, PrivilegeSnapshot] = Map.empty
  )

  /**
   * Snapshot of privilege state values at a point in time.
   */
  case class PrivilegeSnapshot(
    manager: Long,
    delegator: Long,
    registrar: Long,
    assigners: List[Long],
    alwaysAccers: Map[Long, Long]
  )

  /**
   * Outer accumulation function.
   * Recursively processes work reports and deferred transfers.
   */
  private def outerAccumulate(
    partialState: PartialState,
    transfers: List[DeferredTransfer],
    workReports: List[WorkReport],
    alwaysAccers: Map[Long, Long],
    gasLimit: Long,
    timeslot: Long,
    entropy: JamBytes,
    executor: AccumulationExecutor,
    config: ChainConfig
  ): OuterAccumulationResult =
    // Count how many reports can fit in gas budget
    var i = 0
    var sumGasRequired = 0L

    val reportIterator = workReports.iterator
    var continue = true
    while reportIterator.hasNext && continue do
      val report = reportIterator.next()
      var canAccumulate = true
      for result <- report.results if canAccumulate do
        if result.accumulateGas.toLong + sumGasRequired > gasLimit then
          canAccumulate = false
        else
          sumGasRequired += result.accumulateGas.toLong
      if canAccumulate then
        i += 1
      else
        continue = false

    val n = i + transfers.size + alwaysAccers.size

    if n == 0 then
      return OuterAccumulationResult(
        reportsAccumulated = 0,
        postState = partialState,
        gasUsedMap = Map.empty,
        commitments = Set.empty,
        privilegeSnapshots = Map.empty
      )

    // Execute parallel accumulation for this batch
    val parallelResult = executeAccumulation(
      partialState = partialState,
      reports = workReports.take(i),
      deferredTransfers = transfers,
      alwaysAccers = alwaysAccers,
      timeslot = timeslot,
      entropy = entropy,
      executor = executor,
      config = config
    )

    val parallelGasUsed = parallelResult.gasUsedMap.values.sum
    val transfersGas = transfers.map(_.gasLimit).sum

    // Recursively process remaining reports with new deferred transfers
    val remainingReports = workReports.drop(i)
    val newTransfers = parallelResult.deferredTransfers

    // Recursive call if there are new transfers or remaining reports
    val outerResult = outerAccumulate(
      partialState = parallelResult.postState,
      transfers = newTransfers,
      workReports = remainingReports,
      alwaysAccers = Map.empty, // Always-accumulate services only processed in first iteration
      gasLimit = gasLimit + transfersGas - parallelGasUsed,
      timeslot = timeslot,
      entropy = entropy,
      executor = executor,
      config = config
    )

    // Merge results
    val mergedGasUsed = (parallelResult.gasUsedMap.keys ++ outerResult.gasUsedMap.keys).toSet.map { serviceId =>
      serviceId -> (parallelResult.gasUsedMap.getOrElse(serviceId, 0L) + outerResult.gasUsedMap.getOrElse(
        serviceId,
        0L
      ))
    }.toMap

    // Merge privilege snapshots - first snapshot for each service takes precedence
    val mergedSnapshots = parallelResult.privilegeSnapshots ++
      outerResult.privilegeSnapshots.filterKeys(!parallelResult.privilegeSnapshots.contains(_))

    OuterAccumulationResult(
      reportsAccumulated = i + outerResult.reportsAccumulated,
      postState = outerResult.postState,
      gasUsedMap = mergedGasUsed,
      commitments = parallelResult.commitments ++ outerResult.commitments,
      privilegeSnapshots = mergedSnapshots
    )

  /**
   * Result of parallel accumulation execution.
   */
  case class AccumulationExecResult(
    postState: PartialState,
    gasUsedMap: Map[Long, Long],
    commitments: Set[Commitment],
    deferredTransfers: List[DeferredTransfer] = List.empty,
    privilegeSnapshots: Map[Long, PrivilegeSnapshot] = Map.empty
  )

  /**
   * Execute PVM accumulation for all reports.
   */
  private def executeAccumulation(
    partialState: PartialState,
    reports: List[WorkReport],
    deferredTransfers: List[DeferredTransfer],
    alwaysAccers: Map[Long, Long],
    timeslot: Long,
    entropy: JamBytes,
    executor: AccumulationExecutor,
    config: ChainConfig
  ): AccumulationExecResult =
    val gasUsedMap = mutable.Map.empty[Long, Long]
    val commitments = mutable.Set.empty[Commitment]
    val newDeferredTransfers = mutable.ListBuffer.empty[DeferredTransfer]
    val allProvisions = mutable.Set.empty[(Long, JamBytes)]
    val initialState = partialState.deepCopy()

    // Group work items by service
    val serviceOperands = mutable.Map.empty[Long, mutable.ListBuffer[AccumulationOperand]]

    for report <- reports do
      for result <- report.results do
        val operand = OperandTuple(
          packageHash = JamBytes(report.packageSpec.hash.bytes.toArray),
          segmentRoot = JamBytes(report.packageSpec.exportsRoot.bytes.toArray),
          authorizerHash = JamBytes(report.authorizerHash.bytes.toArray),
          payloadHash = JamBytes(result.payloadHash.bytes.toArray),
          gasLimit = result.accumulateGas.toLong,
          authTrace = report.authOutput,
          result = result.result,
          codeHash = JamBytes(result.codeHash.bytes.toArray)
        )
        serviceOperands.getOrElseUpdate(result.serviceId.value.toLong, mutable.ListBuffer.empty) +=
          AccumulationOperand.WorkItem(operand)

    // Add deferred transfers as operands
    for transfer <- deferredTransfers do
      serviceOperands.getOrElseUpdate(transfer.destination, mutable.ListBuffer.empty) +=
        AccumulationOperand.Transfer(transfer)

    // Collect all services to accumulate
    val servicesToAccumulate = mutable.Set.empty[Long]
    servicesToAccumulate ++= serviceOperands.keys
    servicesToAccumulate ++= alwaysAccers.keys

    if servicesToAccumulate.isEmpty then
      return AccumulationExecResult(partialState, Map.empty, Set.empty, List.empty)

    // Track privilege snapshots
    val privilegeSnapshots = mutable.Map.empty[Long, PrivilegeSnapshot]

    // Collect account changes from all services for merging
    val allAccountChanges = new AccountChanges()

    // Execute services sequentially (for now - can be parallelized later)
    val sortedServices = servicesToAccumulate.toList.sorted

    for serviceId <- sortedServices do
      val operands = serviceOperands.getOrElse(serviceId, mutable.ListBuffer.empty).toList
      val alwaysAccGas = alwaysAccers.getOrElse(serviceId, 0L)
      val workItemGas = operands.collect { case AccumulationOperand.WorkItem(op) => op.gasLimit }.sum
      val transferGas = operands.collect { case AccumulationOperand.Transfer(t) => t.gasLimit }.sum
      val totalGasLimit = workItemGas + alwaysAccGas + transferGas

      val serviceInitialState = initialState.deepCopy()

      val execResult = executor.executeService(
        partialState = serviceInitialState,
        timeslot = timeslot,
        serviceId = serviceId,
        gasLimit = totalGasLimit,
        entropy = entropy,
        operands = operands
      )

      // Compute changes this service made
      val serviceChanges = computeServiceChanges(serviceId, initialState, execResult.postState)

      // Merge changes
      allAccountChanges.checkAndMerge(serviceChanges)

      val prevGas = gasUsedMap.getOrElse(serviceId, 0L)
      val newGas = prevGas + execResult.gasUsed
      gasUsedMap(serviceId) = newGas

      // Capture privilege snapshot
      privilegeSnapshots(serviceId) = PrivilegeSnapshot(
        manager = execResult.postState.manager,
        delegator = execResult.postState.delegator,
        registrar = execResult.postState.registrar,
        assigners = execResult.postState.assigners.toList,
        alwaysAccers = execResult.postState.alwaysAccers.toMap
      )

      // Collect yield/commitment if present
      execResult.yieldHash.foreach(hash => commitments += Commitment(serviceId, hash))

      // Collect new deferred transfers
      newDeferredTransfers ++= execResult.deferredTransfers

      // Collect provisions
      allProvisions ++= execResult.provisions

    // Apply all merged account changes to the initial state
    val finalState = initialState.deepCopy()
    allAccountChanges.applyTo(finalState)

    // Process preimage integrations on the final merged state
    val stateAfterPreimages = if allProvisions.nonEmpty then
      preimageIntegration(allProvisions.toSet, finalState, timeslot)
    else
      finalState

    AccumulationExecResult(
      stateAfterPreimages,
      gasUsedMap.toMap,
      commitments.toSet,
      newDeferredTransfers.toList,
      privilegeSnapshots.toMap
    )

  /**
   * Compute changes a service made to state.
   */
  private def computeServiceChanges(
    serviceId: Long,
    initialState: PartialState,
    postState: PartialState
  ): AccountChanges =
    val changes = new AccountChanges()

    // Check for changes in the service's own account
    postState.accounts.get(serviceId).foreach { postAccount =>
      val initAccount = initialState.accounts.get(serviceId)
      if initAccount.isEmpty || initAccount.get != postAccount then
        changes.accountUpdates(serviceId) = postAccount
    }

    // Check for changes in other accounts
    for (id, postAccount) <- postState.accounts if id != serviceId do
      val initAccount = initialState.accounts.get(id)
      if initAccount.isEmpty || initAccount.get != postAccount then
        if !changes.accountUpdates.contains(id) then
          changes.accountUpdates(id) = postAccount

    // Check for removed accounts (accounts that existed in initial but not in post)
    for (id, _) <- initialState.accounts do
      if !postState.accounts.contains(id) then
        changes.removedAccounts += id

    // Check for privilege changes
    if postState.manager != initialState.manager then
      changes.managerChange = Some(postState.manager)
    if postState.delegator != initialState.delegator then
      changes.delegatorChange = Some(postState.delegator)
    if postState.registrar != initialState.registrar then
      changes.registrarChange = Some(postState.registrar)
    if postState.assigners.toList != initialState.assigners.toList then
      changes.assignersChange = Some(postState.assigners.toList)
    if postState.alwaysAccers.toMap != initialState.alwaysAccers.toMap then
      changes.alwaysAccersChange = Some(postState.alwaysAccers.toMap)

    changes

  /**
   * Preimage integration function.
   */
  private def preimageIntegration(
    provisions: Set[(Long, JamBytes)],
    state: PartialState,
    timeslot: Long
  ): PartialState =
    for (serviceId, preimage) <- provisions do
      state.accounts.get(serviceId).foreach { account =>
        // Hash the preimage
        val preimageHash = Hashing.blake2b256(preimage.toArray)
        val preimageHashAsHash = Hash(preimageHash.bytes.toArray)
        val preimageHashBytes = JamBytes(preimageHash.bytes.toArray)
        val length = preimage.length

        // Look up the preimage info entry
        val preimageKey = PreimageKey(preimageHashAsHash, length)
        account.preimageRequests.get(preimageKey).foreach { info =>
          if info.requestedAt.isEmpty then
            // Update preimage info with current timeslot
            account.preimageRequests(preimageKey) = PreimageRequest(List(timeslot))
            // Store the preimage blob
            account.preimages(preimageHashAsHash) = preimage

            // Update raw state data
            val infoStateKey = StateKey.computePreimageInfoStateKey(serviceId, length, preimageHashBytes)
            state.rawServiceDataByStateKey(infoStateKey) = StateKey.encodePreimageInfoValue(List(timeslot))

            val blobStateKey = StateKey.computeServiceDataStateKey(serviceId, 0xfffffffeL, preimageHashBytes)
            state.rawServiceDataByStateKey(blobStateKey) = preimage
        }
      }
    state

  /**
   * Build fresh service statistics for this slot's activity.
   * Statistics are NOT cumulative - they represent only this slot's accumulation.
   * Only services that actually accumulated (gasUsed > 0 or workItems > 0) are included.
   */
  private def updateStatistics(
    existing: List[ServiceStatisticsEntry],
    gasUsedPerService: Map[Long, Long],
    workItemsPerService: Map[Long, Int]
  ): List[ServiceStatisticsEntry] =
    // Build fresh statistics from only this slot's activity
    val statsMap = mutable.Map.empty[Long, ServiceStatisticsEntry]

    for (serviceId, gasUsed) <- gasUsedPerService do
      val workItems = workItemsPerService.getOrElse(serviceId, 0)
      // Only include services that actually did something
      if gasUsed > 0 || workItems > 0 then
        statsMap(serviceId) = ServiceStatisticsEntry(
          id = serviceId,
          record = ServiceActivityRecord(
            accumulateCount = workItems,
            accumulateGasUsed = gasUsed
          )
        )

    statsMap.values.toList.sortBy(_.id)

  /**
   * Count work items per service from accumulated reports.
   */
  private def countWorkItemsPerService(reports: List[WorkReport]): Map[Long, Int] =
    reports.flatMap(_.results.map(_.serviceId.value.toLong))
      .groupBy(identity)
      .view.mapValues(_.size)
      .toMap

  /**
   * Compute the Keccak Merkle root of service commitments.
   */
  private def computeCommitmentRoot(commitments: Set[Commitment]): JamBytes =
    if commitments.isEmpty then
      return JamBytes(new Array[Byte](32))

    // Sort by service index, then by hash for deterministic ordering
    val sortedCommitments = commitments.toList.sortBy(c => (c.serviceIndex, c.hash.toHex))
    val nodes = sortedCommitments.map { commitment =>
      val buffer = ByteBuffer.allocate(4 + 32).order(ByteOrder.LITTLE_ENDIAN)
      buffer.putInt(commitment.serviceIndex.toInt)
      buffer.put(commitment.hash.toArray)
      buffer.array()
    }

    // Binary Merkle tree with Keccak-256
    JamBytes(binaryMerklize(nodes))

  /**
   * Well-balanced binary Merkle function.
   */
  private def binaryMerklize(leaves: List[Array[Byte]]): Array[Byte] =
    leaves match
      case Nil => new Array[Byte](32)
      case head :: Nil => keccak256(head)
      case _ =>
        binaryMerklizeHelper(leaves) match
          case MerklizeResult.Leaf(data) => keccak256(data)
          case MerklizeResult.Hash(hash) => hash

  /**
   * Merkle result can be either a leaf (unhashed data) or a hash.
   */
  private enum MerklizeResult:
    case Leaf(data: Array[Byte])
    case Hash(hash: Array[Byte])

    def toByteArray: Array[Byte] = this match
      case Leaf(data) => data
      case Hash(hash) => hash

  /**
   * Helper for well-balanced binary Merkle tree.
   */
  private def binaryMerklizeHelper(nodes: List[Array[Byte]]): MerklizeResult =
    nodes match
      case Nil => MerklizeResult.Hash(new Array[Byte](32))
      case head :: Nil => MerklizeResult.Leaf(head)
      case _ =>
        val mid = (nodes.size + 1) / 2 // roundup of half
        val left = nodes.take(mid)
        val right = nodes.drop(mid)
        val leftResult = binaryMerklizeHelper(left)
        val rightResult = binaryMerklizeHelper(right)
        // Hash with "node" prefix as per GP E.1.1
        MerklizeResult.Hash(
          keccakHashWithPrefix(
            "node".getBytes,
            leftResult.toByteArray,
            rightResult.toByteArray
          )
        )

  private def keccak256(data: Array[Byte]): Array[Byte] =
    val digest = new Keccak.Digest256()
    digest.update(data, 0, data.length)
    digest.digest()

  private def keccakHashWithPrefix(prefix: Array[Byte], left: Array[Byte], right: Array[Byte]): Array[Byte] =
    val digest = new Keccak.Digest256()
    digest.update(prefix, 0, prefix.length)
    digest.update(left, 0, left.length)
    digest.update(right, 0, right.length)
    digest.digest()

/**
 * Account changes tracker for merging parallel service executions.
 */
class AccountChanges:
  val accountUpdates: mutable.Map[Long, ServiceAccount] = mutable.Map.empty
  val removedAccounts: mutable.Set[Long] = mutable.Set.empty
  var managerChange: Option[Long] = None
  var delegatorChange: Option[Long] = None
  var registrarChange: Option[Long] = None
  var assignersChange: Option[List[Long]] = None
  var alwaysAccersChange: Option[Map[Long, Long]] = None

  def checkAndMerge(other: AccountChanges): Unit =
    // Merge account updates
    for (id, account) <- other.accountUpdates do
      if !accountUpdates.contains(id) then
        accountUpdates(id) = account

    // Merge removed accounts
    removedAccounts ++= other.removedAccounts

    // Merge privilege changes
    if other.managerChange.isDefined && managerChange.isEmpty then
      managerChange = other.managerChange
    if other.delegatorChange.isDefined && delegatorChange.isEmpty then
      delegatorChange = other.delegatorChange
    if other.registrarChange.isDefined && registrarChange.isEmpty then
      registrarChange = other.registrarChange
    if other.assignersChange.isDefined && assignersChange.isEmpty then
      assignersChange = other.assignersChange
    if other.alwaysAccersChange.isDefined && alwaysAccersChange.isEmpty then
      alwaysAccersChange = other.alwaysAccersChange

  def applyTo(state: PartialState): Unit =
    // Apply account removals first (before updates)
    for id <- removedAccounts do
      state.accounts.remove(id)

    // Apply account updates
    for (id, account) <- accountUpdates do
      state.accounts(id) = account

    // Apply privilege changes
    managerChange.foreach(state.manager = _)
    delegatorChange.foreach(state.delegator = _)
    registrarChange.foreach(state.registrar = _)
    assignersChange.foreach { assigners =>
      state.assigners.clear()
      state.assigners ++= assigners
    }
    alwaysAccersChange.foreach { alwaysAccers =>
      state.alwaysAccers.clear()
      state.alwaysAccers ++= alwaysAccers
    }
