package io.forge.jam.core.serializers

import io.forge.jam.core.hexToBytes
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object ByteArrayMapSerializer : KSerializer<Map<ByteArray, ByteArray>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ByteArrayMap")

    override fun deserialize(decoder: Decoder): Map<ByteArray, ByteArray> {
        require(decoder is JsonDecoder)
        val jsonObject = decoder.decodeJsonElement().jsonObject

        return jsonObject.entries.associate { (key, value) ->
            key.hexToBytes() to value.toString().trim('"').hexToBytes()
        }
    }

    override fun serialize(encoder: Encoder, value: Map<ByteArray, ByteArray>) {
        require(encoder is JsonEncoder)
        val jsonObject = JsonObject(value.map { (key, value) ->
            byteArrayToHex(key) to encoder.json.encodeToJsonElement(byteArrayToHex(value))
        }.toMap())
        encoder.encodeJsonElement(jsonObject)
    }

    // Helper function to convert ByteArray to hex string
    private fun byteArrayToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }
}
