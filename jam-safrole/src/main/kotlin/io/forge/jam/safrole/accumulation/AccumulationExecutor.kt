package io.forge.jam.safrole.accumulation

import io.forge.jam.core.JamByteArray
import io.forge.jam.core.WorkReport
import io.forge.jam.pvm.engine.*
import io.forge.jam.pvm.program.ArcBytes
import io.forge.jam.pvm.program.ProgramBlob
import io.forge.jam.pvm.program.ProgramCounter
import io.forge.jam.pvm.program.ProgramParts
import io.forge.jam.pvm.program.Reg
import io.forge.jam.safrole.report.ServiceInfo

/**
 * Orchestrates PVM execution for accumulation.
 * Handles service code loading, execution, and state management.
 */
class AccumulationExecutor(
    private val config: AccumulationConfig
) {
    private val engine: Engine
    private val moduleCache: MutableMap<JamByteArray, Module> = mutableMapOf()

    init {
        val engineConfig = Config.new(allowDynamicPaging = false)
        engine = Engine.new(engineConfig).getOrThrow()
    }

    /**
     * Execute accumulation for a single service.
     * Implements the Ψ_A function from Gray Paper.
     */
    fun executeService(
        partialState: PartialState,
        timeslot: Long,
        serviceId: Long,
        gasLimit: Long,
        entropy: JamByteArray,
        operands: List<AccumulationOperand>
    ): AccumulationOneResult {
        val account = partialState.accounts[serviceId]
            ?: return createEmptyResult(partialState)

        // Get service code from preimages
        val codeHash = account.info.codeHash
        val code = account.preimages[codeHash]?.bytes
            ?: return createEmptyResult(partialState)

        // Check code size limit
        if (code.size > MAX_SERVICE_CODE_SIZE) {
            return createEmptyResult(partialState)
        }

        // Apply incoming transfers to balance
        val transferBalance = operands.filterIsInstance<AccumulationOperand.Transfer>()
            .sumOf { it.transfer.amount }
        val updatedAccount = account.copy(
            info = account.info.copy(balance = account.info.balance + transferBalance)
        )
        val postTransferState = partialState.copy().also {
            it.accounts[serviceId] = updatedAccount
        }

        // Create accumulation context with dual state
        val context = AccumulationContext(
            x = postTransferState.deepCopy(),
            y = postTransferState.deepCopy(),
            serviceIndex = serviceId,
            timeslot = timeslot,
            entropy = entropy
        )

        // Execute PVM
        val (exitReason, gasUsed) = executePvm(context, code, gasLimit, operands)

        // Collapse state based on exit reason
        val finalState = context.collapse(exitReason)

        // Update last_accumulation_slot for this service
        val serviceAccount = finalState.accounts[serviceId]
        if (serviceAccount != null) {
            finalState.accounts[serviceId] = serviceAccount.copy(
                info = serviceAccount.info.copy(lastAccumulationSlot = timeslot)
            )
        }

        return AccumulationOneResult(
            postState = finalState,
            deferredTransfers = context.deferredTransfers.toList(),
            yield = null, // TODO: Capture yield from PVM execution
            gasUsed = gasUsed,
            provisions = context.provisions.toSet()
        )
    }

    /**
     * Execute PVM code with host call handling.
     */
    private fun executePvm(
        context: AccumulationContext,
        code: ByteArray,
        gasLimit: Long,
        operands: List<AccumulationOperand>
    ): Pair<ExitReason, Long> {
        val module = getOrCompileModule(code)
            ?: return Pair(ExitReason.PANIC, 0L)

        val instance = module.instantiate().getOrNull()
            ?: return Pair(ExitReason.PANIC, 0L)

        // Set up host call handler
        val hostCalls = AccumulationHostCalls(context, operands, config)

        // Set initial gas
        instance.setGas(gasLimit)
        val initialGas = gasLimit

        // Prepare call - entry point 5 for accumulate, args: (timeslot, service_id, operand_count)
        instance.prepareCallUntyped(
            ProgramCounter(5u), // Accumulate entry point
            listOf(
                context.timeslot.toULong(),
                context.serviceIndex.toULong(),
                operands.size.toULong()
            )
        )

        // Execute until completion
        var exitReason = ExitReason.HALT
        while (true) {
            when (val interrupt = instance.run().getOrNull()) {
                InterruptKind.Finished -> {
                    exitReason = ExitReason.HALT
                    break
                }
                InterruptKind.Panic -> {
                    exitReason = ExitReason.PANIC
                    break
                }
                InterruptKind.NotEnoughGas -> {
                    exitReason = ExitReason.OUT_OF_GAS
                    break
                }
                is InterruptKind.Ecalli -> {
                    // Handle host call
                    val hostCallGas = hostCalls.dispatch(interrupt.value, instance)
                    // Gas is already deducted by the handler
                    // Continue execution
                }
                is InterruptKind.Segfault -> {
                    exitReason = ExitReason.PAGE_FAULT
                    break
                }
                InterruptKind.Step -> {
                    // Continue for step tracing
                }
                null -> {
                    exitReason = ExitReason.PANIC
                    break
                }
            }
        }

        val gasUsed = initialGas - instance.gas()
        return Pair(exitReason, gasUsed)
    }

    /**
     * Get or compile a module from code bytes.
     */
    private fun getOrCompileModule(code: ByteArray): Module? {
        val codeHash = JamByteArray(blake2b256(code))

        return moduleCache.getOrPut(codeHash) {
            try {
                val parts = ProgramParts.fromBytes(ArcBytes.fromStatic(code)).getOrNull()
                    ?: return null
                val blob = ProgramBlob.fromParts(parts).getOrNull()
                    ?: return null

                val moduleConfig = ModuleConfig.new(dynamicPaging = false)
                moduleConfig.setGasMetering(GasMeteringKind.Sync)
                moduleConfig.setPageSize(4096u)

                Module.fromBlob(engine, moduleConfig, blob).getOrNull()
                    ?: return null
            } catch (e: Exception) {
                return null
            }
        }
    }

    /**
     * Create empty result when service code cannot be executed.
     */
    private fun createEmptyResult(state: PartialState): AccumulationOneResult {
        return AccumulationOneResult(
            postState = state,
            deferredTransfers = emptyList(),
            yield = null,
            gasUsed = 0L,
            provisions = emptySet()
        )
    }

    companion object {
        const val MAX_SERVICE_CODE_SIZE = 4 * 1024 * 1024 // 4MB

        /**
         * Simple Blake2b-256 hash (placeholder - should use proper implementation).
         */
        private fun blake2b256(data: ByteArray): ByteArray {
            // Use the crypto implementation from jam-core
            return org.bouncycastle.crypto.digests.Blake2bDigest(256).let { digest ->
                digest.update(data, 0, data.size)
                val result = ByteArray(32)
                digest.doFinal(result, 0)
                result
            }
        }
    }
}

/**
 * Execute sequential accumulation (accseq from Gray Paper).
 * Processes work reports sequentially, respecting gas budget.
 */
fun accumulateSequential(
    gasLimit: Long,
    deferredTransfers: List<DeferredTransfer>,
    reports: List<WorkReport>,
    partialState: PartialState,
    freeGas: Map<Long, Long>,
    executor: AccumulationExecutor,
    timeslot: Long,
    entropy: JamByteArray,
    config: AccumulationConfig
): AccumulationSeqResult {
    if (reports.isEmpty() && deferredTransfers.isEmpty() && freeGas.isEmpty()) {
        return AccumulationSeqResult(
            reportsAccumulated = 0,
            postState = partialState,
            outputs = emptyMap(),
            gasUsed = emptyList()
        )
    }

    // Calculate how many reports can fit in gas budget
    var totalGas = 0L
    var reportsToProcess = 0
    for (report in reports) {
        val reportGas = report.results.sumOf { it.accumulateGas }
        if (totalGas + reportGas <= gasLimit) {
            totalGas += reportGas
            reportsToProcess++
        } else {
            break
        }
    }

    // Execute parallel accumulation for this batch
    val (newState, newTransfers, outputs, gasUsedList) = accumulateParallel(
        partialState = partialState,
        deferredTransfers = deferredTransfers,
        reports = reports.take(reportsToProcess),
        freeGas = freeGas,
        executor = executor,
        timeslot = timeslot,
        entropy = entropy,
        config = config
    )

    // Recursively process remaining reports with updated gas budget and transfers
    val remainingGas = gasLimit - gasUsedList.sumOf { it.second } + deferredTransfers.sumOf { it.gasLimit }

    if (reportsToProcess < reports.size && remainingGas > 0) {
        val recursiveResult = accumulateSequential(
            gasLimit = remainingGas,
            deferredTransfers = newTransfers,
            reports = reports.drop(reportsToProcess),
            partialState = newState,
            freeGas = emptyMap(), // Free gas only applies to first iteration
            executor = executor,
            timeslot = timeslot,
            entropy = entropy,
            config = config
        )

        return AccumulationSeqResult(
            reportsAccumulated = reportsToProcess + recursiveResult.reportsAccumulated,
            postState = recursiveResult.postState,
            outputs = outputs + recursiveResult.outputs,
            gasUsed = gasUsedList + recursiveResult.gasUsed
        )
    }

    return AccumulationSeqResult(
        reportsAccumulated = reportsToProcess,
        postState = newState,
        outputs = outputs,
        gasUsed = gasUsedList
    )
}

/**
 * Execute parallel accumulation (accpar from Gray Paper).
 * Aggregates work items per service and executes in parallel.
 */
fun accumulateParallel(
    partialState: PartialState,
    deferredTransfers: List<DeferredTransfer>,
    reports: List<WorkReport>,
    freeGas: Map<Long, Long>,
    executor: AccumulationExecutor,
    timeslot: Long,
    entropy: JamByteArray,
    config: AccumulationConfig
): AccumulationParResult {
    // Collect all services that need accumulation
    val services = mutableSetOf<Long>()

    // Services from work reports
    reports.forEach { report ->
        report.results.forEach { result ->
            services.add(result.serviceId)
        }
    }

    // Services from free gas
    services.addAll(freeGas.keys)

    // Services from transfers
    deferredTransfers.forEach { transfer ->
        services.add(transfer.destination)
    }

    // Execute each service
    val allOutputs = mutableMapOf<Long, JamByteArray>()
    val allGasUsed = mutableListOf<Pair<Long, Long>>()
    val allTransfers = mutableListOf<DeferredTransfer>()
    var currentState = partialState

    for (serviceId in services.sorted()) {
        // Build operands for this service
        val operands = mutableListOf<AccumulationOperand>()

        // Add transfers destined for this service
        deferredTransfers.filter { it.destination == serviceId }.forEach {
            operands.add(AccumulationOperand.Transfer(it))
        }

        // Add work items for this service
        val tuples = extractOperandTuples(reports, serviceId)
        tuples.forEach {
            operands.add(AccumulationOperand.WorkItem(it))
        }

        // Calculate gas limit
        val gasLimit = calculateServiceGasLimit(reports, deferredTransfers, freeGas, serviceId)

        // Execute
        val result = executor.executeService(
            partialState = currentState,
            timeslot = timeslot,
            serviceId = serviceId,
            gasLimit = gasLimit,
            entropy = entropy,
            operands = operands
        )

        // Merge results
        currentState = result.postState
        allGasUsed.add(Pair(serviceId, result.gasUsed))
        allTransfers.addAll(result.deferredTransfers)
        result.yield?.let { allOutputs[serviceId] = it }
    }

    return AccumulationParResult(
        postState = currentState,
        deferredTransfers = allTransfers,
        outputs = allOutputs,
        gasUsed = allGasUsed
    )
}
