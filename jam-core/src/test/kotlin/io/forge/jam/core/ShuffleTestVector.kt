package io.forge.jam.core

import io.forge.jam.core.serializers.ByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class ShuffleTestVector(
    val input: Int,
    @Serializable(with = ByteArrayHexSerializer::class)
    val entropy: ByteArray,
    val output: List<Int>
)
