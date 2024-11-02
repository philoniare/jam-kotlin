package io.forge.jam.ec

import io.forge.jam.core.serializers.ByteArrayHexSerializer
import io.forge.jam.core.serializers.ByteArrayListHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class EcData(
    @Serializable(with = ByteArrayHexSerializer::class)
    val data: ByteArray,
    @Serializable(with = ByteArrayListHexSerializer::class)
    val chunks: List<ByteArray>
)
