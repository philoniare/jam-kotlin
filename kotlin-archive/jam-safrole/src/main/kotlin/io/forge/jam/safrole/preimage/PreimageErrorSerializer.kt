package io.forge.jam.safrole.preimage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object PreimageErrorSerializer : KSerializer<PreimageErrorCode> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PreimageErrorCode", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PreimageErrorCode) {
        val serialName = value.name.lowercase().replace('_', '-')
        encoder.encodeString(serialName)
    }

    override fun deserialize(decoder: Decoder): PreimageErrorCode {
        val string = decoder.decodeString()
        return PreimageErrorCode.values().find { errorCode ->
            val hyphenName = errorCode.name.lowercase().replace('_', '-')
            val underscoreName = errorCode.name.lowercase().replace('-', '_')
            string == hyphenName || string == underscoreName
        } ?: throw SerializationException("Unknown error code: $string")
    }
}

