package io.forge.jam.safrole.report

import io.forge.jam.core.Encodable
import kotlinx.serialization.Serializable

@Serializable
data class ReportOutput(
    val ok: ReportOutputMarks? = null,
    @Serializable(with = ReportErrorCodeSerializer::class)
    val err: ReportErrorCode? = null
) : Encodable {
    override fun encode(): ByteArray {
        return if (ok != null) {
            // Prepend a 0 byte to indicate "ok" choice
            byteArrayOf(0) + ok.encode()
        } else {
            // For error case, prepend a 1 byte to indicate "err" choice
            byteArrayOf(1) + err!!.encode()
        }
    }
}
