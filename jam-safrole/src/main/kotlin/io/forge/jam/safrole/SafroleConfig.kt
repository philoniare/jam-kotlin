package io.forge.jam.safrole

data class SafroleConfig(
    val epochLength: Long,
    val ticketCutoff: Long,
    val ringSize: Int
)
