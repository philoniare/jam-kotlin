package io.forge.jam.safrole.preimage

import io.forge.jam.core.Encodable
import io.forge.jam.safrole.report.ReportOutputMarks
import kotlinx.serialization.Serializable

@Serializable
data class PreimageOutput(
    val ok: ReportOutputMarks? = null,
    @Serializable(with = PreimageErrorSerializer::class)
    val err: PreimageErrorCode? = null
) : Encodable {
    override fun encode(): ByteArray {
        return if (ok != null) {
            // Prepend a 0 byte to indicate "ok" choice
            byteArrayOf(0) + ok.encode()
        } else {
            // For error case, encode without the 0 byte prefix
            err!!.encode()
        }
    }
}
