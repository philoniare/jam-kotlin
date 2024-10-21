package io.forge.jam.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ByteArrayHexSerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HexByteArray", PrimitiveKind.STRING)

    // Deserialize hex string to ByteArray
    override fun deserialize(decoder: Decoder): ByteArray {
        val hexString = decoder.decodeString()
        return hexString.hexToBytes()
    }

    // Serialize ByteArray to hex string
    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(byteArrayToHex(value))
    }

    // Helper function to convert ByteArray to hex string
    private fun byteArrayToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }
}

