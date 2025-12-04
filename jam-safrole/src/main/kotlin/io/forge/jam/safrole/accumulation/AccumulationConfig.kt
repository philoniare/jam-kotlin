package io.forge.jam.safrole.accumulation

data class AccumulationConfig(
    val EPOCH_LENGTH: Int,
    val MAX_BLOCK_HISTORY: Int,
    val AUTH_QUEUE_SIZE: Int,
    val PREIMAGE_PURGE_PERIOD: Long = 32L,
    val SERVICE_MIN_BALANCE: Long = 100L,
    val ADDITIONAL_MIN_BALANCE_PER_STATE_ITEM: Long = 10L,
    val ADDITIONAL_MIN_BALANCE_PER_STATE_BYTE: Long = 1L
)
