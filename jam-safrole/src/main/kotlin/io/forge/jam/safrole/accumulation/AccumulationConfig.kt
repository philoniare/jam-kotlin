package io.forge.jam.safrole.accumulation

data class AccumulationConfig(
    val EPOCH_LENGTH: Int,
    val MAX_BLOCK_HISTORY: Int,
    val AUTH_QUEUE_SIZE: Int
)
