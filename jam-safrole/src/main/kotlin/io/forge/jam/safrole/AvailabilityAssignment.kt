package io.forge.jam.safrole

import io.forge.jam.core.Encodable
import io.forge.jam.core.WorkReport
import io.forge.jam.core.encodeFixedWidthInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AvailabilityAssignment(
    @SerialName("report")
    val report: WorkReport,

    val timeout: Long
) : Encodable {
    override fun encode(): ByteArray {
        val reportBytes = report.encode()
        val timeoutBytes = encodeFixedWidthInteger(timeout, 4, true)
        return reportBytes + timeoutBytes
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AvailabilityAssignment) return false

        if (report != other.report) return false
        if (timeout != other.timeout) return false

        return true
    }

    override fun hashCode(): Int {
        var result = report.hashCode()
        result = 31 * result + timeout.hashCode()
        return result
    }
}
