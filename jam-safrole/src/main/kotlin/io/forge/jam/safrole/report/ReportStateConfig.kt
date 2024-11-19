package io.forge.jam.safrole.report

data class ReportStateConfig(
    val MAX_LOOKUP_ANCHOR_AGE: Long,
    val MAX_AUTH_POOL_ITEMS: Int,
    val MAX_ACCUMULATION_GAS: Long
)
