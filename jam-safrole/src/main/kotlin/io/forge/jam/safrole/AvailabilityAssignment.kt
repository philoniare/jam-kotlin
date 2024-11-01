package io.forge.jam.safrole

import io.forge.jam.core.serializers.ByteArrayHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AvailabilityAssignment(
    @SerialName("dummy_work_report")
    @Serializable(with = ByteArrayHexSerializer::class)
    val dummyWorkReport: ByteArray,

    val timeout: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AvailabilityAssignment) return false

        if (!dummyWorkReport.contentEquals(other.dummyWorkReport)) return false
        if (timeout != other.timeout) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dummyWorkReport.contentHashCode()
        result = 31 * result + timeout.hashCode()
        return result
    }
}
