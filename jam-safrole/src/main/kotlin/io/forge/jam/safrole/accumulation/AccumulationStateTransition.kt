package io.forge.jam.safrole.accumulation

import io.forge.jam.core.JamByteArray
import io.forge.jam.core.WorkReport
import io.forge.jam.safrole.report.ServiceItem

class AccumulationStateTransition(private val config: AccumulationConfig) {
    private fun partitionReports(
        reports: List<WorkReport>
    ): Pair<List<WorkReport>, List<WorkReport>> {
        val immediate = reports.filter { report ->
            report.context.prerequisites.isEmpty() &&
                report.segmentRootLookup.isEmpty()
        }
        val queued = reports.filter { report ->
            !immediate.contains(report)
        }
        return Pair(immediate, queued)
    }

    private fun updateReadyQueue(
        newReports: List<WorkReport>,
        readyQueue: List<List<ReadyRecord>>,
        slot: Long,
        accumulatedHashes: Set<JamByteArray>
    ): List<ReadyRecord> {
        // Convert work reports to ready records
        return newReports.map { report ->
            ReadyRecord(
                report = report,
                dependencies = report.context.prerequisites.map {
                    JamByteArray(it.bytes)
                }
            )
        }.filter { record ->
            // Filter out records whose dependencies are already accumulated
            record.dependencies.none { dep ->
                accumulatedHashes.contains(dep)
            }
        }
    }

    private fun getAccumulatableReports(
        immediateReports: List<WorkReport>,
        updatedQueue: List<ReadyRecord>,
        readyQueue: List<List<ReadyRecord>>,
        slot: Long
    ): List<WorkReport> {
        val accumulatable = immediateReports.toMutableList()

        // Add reports from ready queue that have all dependencies satisfied
        readyQueue.forEach { queueSlot ->
            queueSlot.forEach { record ->
                if (record.dependencies.isEmpty()) {
                    accumulatable.add(record.report)
                }
            }
        }

        return accumulatable
    }

    private fun getAccumulatedHashes(
        accumulated: List<List<JamByteArray>>
    ): Set<JamByteArray> {
        return accumulated.flatten().toSet()
    }

    private fun getReportHash(report: WorkReport): ByteArray {
        // For now just return dummy hash
        // TODO: Implement proper report hash calculation
        return byteArrayOf(0)
    }

    private fun updateAccounts(
        accounts: List<ServiceItem>,
        reports: List<WorkReport>
    ) {
        // TODO: Implement account updates based on work report accumulation
    }

    fun transition(
        input: AccumulationInput,
        preState: AccumulationState
    ): Pair<AccumulationState, AccumulationOutput> {
        val postState = preState.deepCopy()

        // Partition work reports into immediate execution and queued
        val (immediateReports, queuedReports) = partitionReports(input.reports)

        // Update ready queue with new queued reports
        val updatedQueue = updateReadyQueue(
            queuedReports,
            postState.readyQueue,
            input.slot % config.EPOCH_LENGTH,
            getAccumulatedHashes(postState.accumulated)
        )
        postState.readyQueue[input.slot.toInt() % config.EPOCH_LENGTH] = updatedQueue

        // Get sequence of accumulatable work reports
        val accumulatableReports = getAccumulatableReports(
            immediateReports,
            updatedQueue,
            postState.readyQueue,
            input.slot
        )

        // Process accumulation of reports
        val m = input.slot % config.EPOCH_LENGTH
        postState.accumulated[m.toInt()] = accumulatableReports.map { report ->
            JamByteArray(getReportHash(report))
        }

        // Update accounts based on accumulation
        updateAccounts(postState.accounts, accumulatableReports)

        postState.slot = input.slot

        return Pair(
            postState,
            AccumulationOutput(
                ok = JamByteArray(ByteArray(32) { 0 })
            )
        )
    }
}
