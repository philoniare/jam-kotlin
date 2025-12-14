package io.forge.jam.protocol.accumulation

import io.forge.jam.core.{ChainConfig, JamBytes, Hashing, codec}
import io.forge.jam.core.primitives.Hash
import io.forge.jam.core.types.service.ServiceInfo
import spire.math.ULong

import scala.collection.mutable

/**
 * TODO: Remove during 0.7.1 transition
 * Handles host calls during on_transfer PVM execution.
 * This is a limited version of AccumulationHostCalls that only supports:
 * - GAS (0): Returns remaining gas
 * - FETCH (1): Fetch operands (transfers), constants, entropy
 * - LOOKUP (2): Look up preimage
 * - READ (3): Read from storage
 * - WRITE (4): Write to storage
 * - INFO (5): Get service account info
 *
 * All other host calls return WHAT error code.
 *
 * @param accounts Mutable map of all service accounts
 * @param transfers The list of transfers to this service
 * @param serviceId The service index receiving the transfers
 * @param timeslot The current timeslot
 * @param entropy The current entropy
 * @param rawServiceDataByStateKey Raw state data lookups
 * @param config The chain configuration
 */
class OnTransferHostCalls(
  val accounts: mutable.Map[Long, ServiceAccount],
  val transfers: List[DeferredTransfer],
  val serviceId: Long,
  val timeslot: Long,
  val entropy: JamBytes,
  val rawServiceDataByStateKey: mutable.Map[JamBytes, JamBytes],
  val config: ChainConfig
):

  /**
   * Get register value from PVM instance as ULong.
   */
  private def getReg(instance: PvmInstance, reg: Int): ULong =
    ULong(instance.reg(reg))

  /**
   * Set register value in PVM instance.
   */
  private def setReg(instance: PvmInstance, reg: Int, value: ULong): Unit =
    instance.setReg(reg, value.signed)

  /**
   * Calculate threshold balance for a service account.
   */
  private def calculateThreshold(items: Int, bytesUsed: Long, depositOffset: Long): Long =
    val base = config.serviceMinBalance
    val itemsCost = config.additionalMinBalancePerStateItem * items
    val bytesCost = config.additionalMinBalancePerStateByte * bytesUsed
    val costUnsigned = ULong(base + itemsCost + bytesCost)
    val gratisUnsigned = ULong(depositOffset)
    if costUnsigned > gratisUnsigned then (costUnsigned - gratisUnsigned).toLong else 0L

  private def calculateThreshold(info: ServiceInfo): Long =
    calculateThreshold(info.items, info.bytesUsed, info.depositOffset)

  private def meetsThreshold(balance: Long, threshold: Long): Boolean =
    ULong(balance) >= ULong(threshold)

  /**
   * Get gas cost for a host call.
   */
  def getGasCost(hostCallId: Int, instance: PvmInstance): Long =
    10L // All allowed host calls have gas cost of 10

  /**
   * Dispatch a host call based on its identifier.
   * Only GAS, FETCH, LOOKUP, READ, WRITE, INFO are allowed.
   * All others return WHAT.
   */
  def dispatch(hostCallId: Int, instance: PvmInstance): Unit =
    hostCallId match
      case HostCall.GAS => handleGas(instance)
      case HostCall.FETCH => handleFetch(instance)
      case HostCall.LOOKUP => handleLookup(instance)
      case HostCall.READ => handleRead(instance)
      case HostCall.WRITE => handleWrite(instance)
      case HostCall.INFO => handleInfo(instance)
      case _ =>
        // All other host calls return WHAT for on_transfer
        setReg(instance, 7, HostCallResult.WHAT)

  /**
   * gas (0): Returns remaining gas in register r7.
   */
  private def handleGas(instance: PvmInstance): Unit =
    setReg(instance, 7, ULong(instance.gas))

  /**
   * fetch (1): Fetch various data based on register r10 selector.
   * For on_transfer, supports fetching transfer operands and constants.
   */
  private def handleFetch(instance: PvmInstance): Unit =
    val selector = getReg(instance, 10).toInt
    val outputAddr = getReg(instance, 7).toInt
    val offset = getReg(instance, 8).toInt
    val length = getReg(instance, 9).toInt
    val index = getReg(instance, 11).toInt

    val data: Option[Array[Byte]] = selector match
      case 0 => Some(getConstantsBlob())
      case 1 => Some(entropy.toArray)
      case 14 => Some(encodeTransfersList())
      case 15 =>
        if index < transfers.size then Some(encodeTransfer(transfers(index)))
        else None
      case _ => None

    data match
      case None =>
        setReg(instance, 7, HostCallResult.NONE)
      case Some(bytes) =>
        val actualOffset = math.min(offset, bytes.length)
        val actualLength = math.min(length, bytes.length - actualOffset)
        val slice = bytes.slice(actualOffset, actualOffset + actualLength)

        if !isMemoryWritable(instance, outputAddr, actualLength) then
          throw new RuntimeException(
            s"Fetch PANIC: Output memory not writable at 0x${outputAddr.toHexString} len $actualLength"
          )

        val writeResult = writeMemory(instance, outputAddr, slice)
        if !writeResult then
          setReg(instance, 7, HostCallResult.OOB)
        else
          setReg(instance, 7, ULong(bytes.length))

  /**
   * lookup (2): Look up preimage by hash.
   */
  private def handleLookup(instance: PvmInstance): Unit =
    val targetServiceId = getReg(instance, 7).toLong
    val hashAddr = getReg(instance, 8).toInt
    val outputAddr = getReg(instance, 9).toInt
    val offset = getReg(instance, 10).toInt
    val length = getReg(instance, 11).toInt

    val hashBuffer = new Array[Byte](32)
    if !readMemory(instance, hashAddr, hashBuffer) then
      throw new RuntimeException(s"Lookup PANIC: Failed to read hash from memory at 0x${hashAddr.toHexString}")

    val hashBytes = JamBytes(hashBuffer)
    val hash = Hash(hashBuffer)

    val resolvedServiceId = if targetServiceId == -1L || targetServiceId == serviceId then
      serviceId
    else
      targetServiceId

    val account = accounts.get(resolvedServiceId)

    var preimage = account.flatMap(_.preimages.get(hash))

    if preimage.isEmpty then
      val blobStateKey = StateKey.computeServiceDataStateKey(resolvedServiceId, 0xfffffffeL, hashBytes)
      preimage = rawServiceDataByStateKey.get(blobStateKey)

    val dataSize = preimage.map(_.length).getOrElse(0)
    val actualOffset = math.min(offset, dataSize)
    val actualLength = math.min(length, dataSize - actualOffset)

    if !isMemoryWritable(instance, outputAddr, actualLength) then
      throw new RuntimeException(
        s"Lookup PANIC: Output memory not writable at 0x${outputAddr.toHexString} len $actualLength"
      )

    if account.isEmpty then
      setReg(instance, 7, HostCallResult.WHO)
      return

    if preimage.isEmpty then
      setReg(instance, 7, HostCallResult.NONE)
      return

    val data = preimage.get.toArray
    val slice = data.slice(actualOffset, actualOffset + actualLength)

    if !writeMemory(instance, outputAddr, slice) then
      setReg(instance, 7, HostCallResult.OOB)
      return

    setReg(instance, 7, ULong(data.length))

  /**
   * read (3): Read from service storage.
   */
  private def handleRead(instance: PvmInstance): Unit =
    val targetServiceId = getReg(instance, 7).toLong
    val keyAddr = getReg(instance, 8).toInt
    val keyLen = getReg(instance, 9).toInt
    val outputAddr = getReg(instance, 10).toInt
    val offset = getReg(instance, 11).toInt
    val length = getReg(instance, 12).toInt

    val keyBuffer = new Array[Byte](keyLen)
    if !readMemory(instance, keyAddr, keyBuffer) then
      throw new RuntimeException(s"Read PANIC: Failed to read key from memory at 0x${keyAddr.toHexString} len $keyLen")

    val key = JamBytes(keyBuffer)

    val resolvedServiceId = if targetServiceId == -1L || targetServiceId == serviceId then
      serviceId
    else
      targetServiceId

    val account = accounts.get(resolvedServiceId)

    if account.isEmpty then
      setReg(instance, 7, HostCallResult.WHO)
      return

    var value = account.flatMap(_.storage.get(key))

    if value.isEmpty then
      val stateKey = StateKey.computeStorageStateKey(resolvedServiceId, key)
      value = rawServiceDataByStateKey.get(stateKey)

    if value.isEmpty then
      setReg(instance, 7, HostCallResult.NONE)
      return

    val data = value.get.toArray
    val actualOffset = math.min(offset, data.length)
    val actualLength = math.min(length, data.length - actualOffset)
    val slice = data.slice(actualOffset, actualOffset + actualLength)

    if !writeMemory(instance, outputAddr, slice) then
      throw new RuntimeException(s"Read PANIC: Failed to write to output memory at 0x${outputAddr.toHexString}")

    setReg(instance, 7, ULong(data.length))

  /**
   * write (4): Write to service storage.
   * Only writes to own account are allowed.
   */
  private def handleWrite(instance: PvmInstance): Unit =
    val keyAddr = getReg(instance, 7).toInt
    val keyLen = getReg(instance, 8).toInt
    val valueAddr = getReg(instance, 9).toInt
    val valueLen = getReg(instance, 10).toInt

    val account = accounts.get(serviceId)
    if account.isEmpty then
      throw new RuntimeException("Write PANIC: Current service account not found")

    val acc = account.get

    val keyBuffer = new Array[Byte](keyLen)
    if !readMemory(instance, keyAddr, keyBuffer) then
      throw new RuntimeException(s"Write PANIC: Failed to read key from memory at $keyAddr len $keyLen")

    val key = JamBytes(keyBuffer)

    // Track old value for return value and bytes calculation
    var oldValue = acc.storage.get(key)
    val stateKeyForLookup = StateKey.computeStorageStateKey(serviceId, key)
    if oldValue.isEmpty then
      oldValue = rawServiceDataByStateKey.get(stateKeyForLookup)

    val oldValueSize = oldValue.map(_.length).getOrElse(0)
    val keyWasPresent = oldValue.isDefined

    val newValue = if valueLen == 0 then None
    else
      val valueBuffer = new Array[Byte](valueLen)
      if !readMemory(instance, valueAddr, valueBuffer) then
        throw new RuntimeException(s"Write PANIC: Failed to read value from memory at $valueAddr len $valueLen")
      Some(JamBytes(valueBuffer))

    val (bytesDelta, itemsDelta): (Long, Int) = (valueLen, keyWasPresent) match
      case (0, true) =>
        (-(keyLen.toLong + oldValueSize + 34), -1)
      case (0, false) =>
        (0L, 0)
      case (_, true) =>
        ((valueLen - oldValueSize).toLong, 0)
      case (_, false) =>
        ((keyLen + valueLen + 34).toLong, 1)

    val info = acc.info
    val newBytes = info.bytesUsed + bytesDelta
    val newItems = info.items + itemsDelta
    val newThreshold = calculateThreshold(newItems, newBytes, info.depositOffset)

    if !meetsThreshold(info.balance, newThreshold) then
      setReg(instance, 7, HostCallResult.FULL)
      return

    val stateKey = StateKey.computeStorageStateKey(serviceId, key)

    if valueLen == 0 then
      if keyWasPresent then
        acc.storage.remove(key)
        rawServiceDataByStateKey.remove(stateKey)
    else
      acc.storage(key) = newValue.get
      rawServiceDataByStateKey(stateKey) = newValue.get

    // Update account info with new bytes/items
    val updatedInfo = info.copy(
      bytesUsed = newBytes,
      items = newItems
    )
    accounts(serviceId) = acc.copy(info = updatedInfo)

    val returnValue = if keyWasPresent then ULong(oldValueSize) else HostCallResult.NONE
    setReg(instance, 7, returnValue)

  /**
   * info (5): Get service account info.
   */
  private def handleInfo(instance: PvmInstance): Unit =
    val targetServiceId = getReg(instance, 7).toLong
    val outputAddr = getReg(instance, 8).toInt
    val offset = getReg(instance, 9).toInt
    val length = getReg(instance, 10).toInt

    val resolvedServiceId = if targetServiceId == -1L then serviceId else targetServiceId
    val account = accounts.get(resolvedServiceId)

    if account.isEmpty then
      setReg(instance, 7, HostCallResult.NONE)
      return

    val info = account.get.info
    val thresholdBalance = calculateThreshold(info)

    val data = info.codeHash.bytes.toArray ++
      encodeLong(info.balance) ++
      encodeLong(thresholdBalance) ++
      encodeLong(info.minItemGas) ++
      encodeLong(info.minMemoGas) ++
      encodeLong(info.bytesUsed) ++
      encodeInt(info.items) ++
      encodeLong(info.depositOffset) ++
      encodeInt(info.creationSlot.toInt) ++
      encodeInt(info.lastAccumulationSlot.toInt) ++
      encodeInt(info.parentService.toInt)

    val first = math.min(offset, data.length)
    val len = math.min(length, data.length - first)
    val slicedData = data.slice(first, first + len)

    if !writeMemory(instance, outputAddr, slicedData) then
      throw new RuntimeException(s"Info PANIC: Failed to write to memory at $outputAddr")

    setReg(instance, 7, ULong(data.length))

  /**
   * Encode protocol configuration as expected by the guest.
   */
  private def getConstantsBlob(): Array[Byte] =
    val buffer = new java.io.ByteArrayOutputStream()
    val isTiny = config.validatorCount == 6

    buffer.write(encodeLong(config.additionalMinBalancePerStateItem))
    buffer.write(encodeLong(config.additionalMinBalancePerStateByte))
    buffer.write(encodeLong(config.serviceMinBalance))
    buffer.write(encodeShort(config.coresCount))
    buffer.write(encodeIntLE(config.preimageExpungePeriod))
    buffer.write(encodeIntLE(config.epochLength))
    buffer.write(encodeLong(config.maxAccumulationGas))
    buffer.write(encodeLong(50_000_000L))
    buffer.write(encodeLong(config.maxRefineGas))
    buffer.write(encodeLong(if isTiny then config.maxBlockGas else 3_500_000_000L))
    buffer.write(encodeShort(config.maxBlockHistory))
    buffer.write(encodeShort(16))
    buffer.write(encodeShort(config.maxDependencies))
    buffer.write(encodeShort(config.maxTicketsPerExtrinsic))
    buffer.write(encodeIntLE(if isTiny then 24 else 14400))
    buffer.write(encodeShort(config.ticketsPerValidator))
    buffer.write(encodeShort(8))
    buffer.write(encodeShort(config.slotDuration))
    buffer.write(encodeShort(config.authQueueSize))
    buffer.write(encodeShort(config.rotationPeriod))
    buffer.write(encodeShort(128))
    buffer.write(encodeShort(5))
    buffer.write(encodeShort(config.validatorCount))
    buffer.write(encodeIntLE(if isTiny then 64000 else 64_000_000))
    buffer.write(encodeIntLE(if isTiny then 13_794_305 else 12_000_000))
    buffer.write(encodeIntLE(if isTiny then 4_000_000 else 5_000_000))
    buffer.write(encodeIntLE(if isTiny then 4 else 12))
    buffer.write(encodeIntLE(3072))
    buffer.write(encodeIntLE(config.numEcPiecesPerSegment))
    buffer.write(encodeIntLE(48 * 1024))
    buffer.write(encodeIntLE(128))
    buffer.write(encodeIntLE(3072))
    buffer.write(encodeIntLE(config.ticketCutoff))

    buffer.toByteArray

  /**
   * Encode the full list of transfers as operands.
   */
  private def encodeTransfersList(): Array[Byte] =
    val buffer = mutable.ListBuffer.empty[Byte]
    buffer ++= codec.encodeCompactInteger(transfers.size.toLong)
    for transfer <- transfers do
      buffer ++= encodeTransfer(transfer)
    buffer.toArray

  /**
   * Encode a single transfer as an operand (variant 1 format).
   */
  private def encodeTransfer(transfer: DeferredTransfer): Array[Byte] =
    AccumulationOperand.Transfer(transfer).encode()

  private inline def encodeLE(value: Long, size: Int): Array[Byte] =
    Array.tabulate(size)(i => ((value >> (i * 8)) & 0xff).toByte)

  private inline def encodeShort(value: Int): Array[Byte] = encodeLE(value, 2)
  private inline def encodeInt(value: Int): Array[Byte] = encodeLE(value, 4)
  private inline def encodeIntLE(value: Int): Array[Byte] = encodeLE(value, 4)
  private inline def encodeLong(value: Long): Array[Byte] = encodeLE(value, 8)

  private def isMemoryWritable(instance: PvmInstance, address: Int, length: Int): Boolean =
    instance.isMemoryAccessible(address, length)

  private def readMemory(instance: PvmInstance, address: Int, buffer: Array[Byte]): Boolean =
    var i = 0
    while i < buffer.length do
      instance.readByte(address + i) match
        case Some(v) => buffer(i) = v
        case None => return false
      i += 1
    true

  private def writeMemory(instance: PvmInstance, address: Int, data: Array[Byte]): Boolean =
    var i = 0
    while i < data.length do
      if !instance.writeByte(address + i, data(i)) then
        return false
      i += 1
    true
