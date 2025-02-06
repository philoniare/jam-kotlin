package io.forge.jam.safrole.accumulation

import io.forge.jam.core.Encodable
import io.forge.jam.core.JamByteArray
import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class AccumulationOutput(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val ok: JamByteArray
) : Encodable {
    override fun encode(): ByteArray {
        return ok.bytes
    }
}
