package io.forge.jam.safrole

import kotlinx.serialization.Serializable

@Serializable
data class HistoricalState(
    val beta: List<HistoricalBeta>
)
