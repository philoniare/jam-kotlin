package io.forge.jam.safrole.dispute

import io.forge.jam.core.Encodable
import io.forge.jam.core.SafroleErrorCode
import io.forge.jam.core.SafroleErrorCodeSerializer
import kotlinx.serialization.Serializable

@Serializable
data class DisputeOutput(
    val ok: DisputeOutputMarks? = null,
    @Serializable(with = SafroleErrorCodeSerializer::class)
    val err: SafroleErrorCode? = null
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
