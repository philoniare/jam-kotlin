package io.forge.jam.safrole.safrole

import io.forge.jam.core.Encodable
import io.forge.jam.core.SafroleErrorCode
import io.forge.jam.core.SafroleErrorCodeSerializer
import io.forge.jam.safrole.OutputMarks
import kotlinx.serialization.Serializable

@Serializable
data class SafroleOutput(
    val ok: OutputMarks? = null,
    @Serializable(with = SafroleErrorCodeSerializer::class)
    val err: SafroleErrorCode? = null
) : Encodable {
    override fun encode(): ByteArray {
        return if (ok != null) {
            ok.encode()
        } else {
            err!!.encode()
        }
    }
}
