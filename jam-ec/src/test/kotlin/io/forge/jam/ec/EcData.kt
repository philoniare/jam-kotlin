package io.forge.jam.ec

import io.forge.jam.core.JamByteArray
import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import io.forge.jam.core.serializers.JamByteArrayListHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class EcData(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val data: JamByteArray,
    @Serializable(with = JamByteArrayListHexSerializer::class)
    val shards: List<JamByteArray>
)
