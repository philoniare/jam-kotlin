package io.forge.jam.safrole.accumulation

import io.forge.jam.core.JamByteArray

class AccumulationStateTransition(private val config: AccumulationConfig) {
    fun transition(
        input: AccumulationInput,
        preState: AccumulationState
    ): Pair<AccumulationState, AccumulationOutput> {
        val postState = preState.deepCopy()
        val previousSlot = postState.slot
        postState.slot = input.slot

        if (input.reports.isEmpty()) {
            return Pair(postState, AccumulationOutput(ok = JamByteArray(ByteArray(32) { 0 })))
        }

        val accumulatedHashes = postState.accumulated.flatten().toMutableSet()

        val immediateReports =
            input.reports.filter { it.context.prerequisites.isEmpty() && it.segmentRootLookup.isEmpty() }
        val queuedReports =
            input.reports.filter { immediateReports.contains(it).not() }

        val newAccumulated = immediateReports.map { it.packageSpec.hash }
        accumulatedHashes.addAll(newAccumulated)

        val slot = input.slot % config.EPOCH_LENGTH
        val newReadyQueueCopy = postState.readyQueue.toMutableList()
        newReadyQueueCopy[slot.toInt()] = queuedReports.map {
            ReadyRecord(
                report = it,
                dependencies = it.context.prerequisites + it.segmentRootLookup.map { sr -> sr.workPackageHash }
            )
        }


        val processedReports = mutableListOf<JamByteArray>()
        var madeProgress: Boolean
        do {
            madeProgress = false
            for (i in newReadyQueueCopy.indices) {
                val slotReports = newReadyQueueCopy[i].toMutableList()
                var j = 0
                while (j < slotReports.size) {
                    val record = slotReports[j]
                    if (record.dependencies.all { dep -> accumulatedHashes.contains(dep) }) {
                        processedReports.add(record.report.packageSpec.hash)
                        accumulatedHashes.add(record.report.packageSpec.hash)
                        slotReports.removeAt(j)
                        madeProgress = true
                    } else {
                        j++
                    }
                }
                newReadyQueueCopy[i] = slotReports
            }
        } while (madeProgress)

        // Update state
        postState.readyQueue = newReadyQueueCopy
        val newAccumulatedAndProcessed = (newAccumulated + processedReports).sortedBy { it.toHex() }
        postState.accumulated = (postState.accumulated.drop(1) + listOf(newAccumulatedAndProcessed)).toMutableList()

        return Pair(postState, AccumulationOutput(ok = JamByteArray(ByteArray(32) { 0 })))
    }
}
