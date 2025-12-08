package io.forge.jam.pvm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PvmStatus {
    @SerialName("panic")
    PANIC,

    @SerialName("halt")
    HALT,

    @SerialName("page-fault")
    PAGE_FAULT;

    companion object {
        fun fromString(value: String): PvmStatus = when (value.lowercase()) {
            "panic" -> PANIC
            "halt" -> HALT
            "page-fault" -> PAGE_FAULT
            else -> throw IllegalArgumentException("Invalid status: $value. Must be either 'panic', 'halt' or 'page-fault'")
        }
    }
}
