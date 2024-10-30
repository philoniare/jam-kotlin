package io.forge.jam.core.serializers

import io.forge.jam.core.hexToBytes
import io.forge.jam.core.toHex
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure

object NullableByteArrayListSerializer : KSerializer<List<ByteArray?>> {
    private object NullableByteArraySerializer : KSerializer<ByteArray?> {
        override val descriptor = buildClassSerialDescriptor("ByteArray?")

        @OptIn(ExperimentalSerializationApi::class)
        override fun serialize(encoder: Encoder, value: ByteArray?) {
            when (value) {
                null -> encoder.encodeNull()
                else -> encoder.encodeString(value.toHex())
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        override fun deserialize(decoder: Decoder): ByteArray? {
            return try {
                decoder.decodeString().hexToBytes()
            } catch (e: SerializationException) {
                null
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = listSerialDescriptor(NullableByteArraySerializer.descriptor)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: List<ByteArray?>) {
        encoder.beginCollection(descriptor, value.size).apply {
            value.forEachIndexed { index, bytes ->
                encodeNullableSerializableElement(
                    descriptor,
                    index,
                    NullableByteArraySerializer,
                    bytes
                )
            }
            endStructure(descriptor)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): List<ByteArray?> {
        return decoder.decodeStructure(descriptor) {
            val list = ArrayList<ByteArray?>()
            while (true) {
                val index = decodeElementIndex(descriptor)
                if (index == CompositeDecoder.DECODE_DONE) break

                list.add(
                    decodeNullableSerializableElement(
                        descriptor,
                        index,
                        NullableByteArraySerializer
                    )
                )
            }
            list
        }
    }
}
