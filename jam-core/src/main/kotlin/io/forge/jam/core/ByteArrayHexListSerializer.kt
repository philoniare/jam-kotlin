package io.forge.jam.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ByteArrayListHexSerializer : KSerializer<List<ByteArray>> {
    // Create a list serializer using the existing ByteArrayHexSerializer
    private val listSerializer = ListSerializer(ByteArrayHexSerializer)

    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun deserialize(decoder: Decoder): List<ByteArray> {
        return listSerializer.deserialize(decoder)
    }

    override fun serialize(encoder: Encoder, value: List<ByteArray>) {
        listSerializer.serialize(encoder, value)
    }
}
