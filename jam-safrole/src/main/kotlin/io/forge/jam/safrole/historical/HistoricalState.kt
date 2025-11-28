package io.forge.jam.safrole.historical

import kotlinx.serialization.Serializable

@Serializable
data class HistoricalState(
    val beta: HistoricalBetaContainer
)

@Serializable
data class HistoricalBetaContainer(
    val history: List<HistoricalBeta> = emptyList(),
    val mmr: HistoricalMmr = HistoricalMmr(emptyList())
)
