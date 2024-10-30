package io.forge.jam.core

import io.forge.jam.core.serializers.ByteArrayHexSerializer
import io.forge.jam.core.serializers.ByteArrayMapSerializer
import kotlinx.serialization.Serializable

@Serializable
data class TrieTestVector(
    @Serializable(with = ByteArrayMapSerializer::class)
    val input: Map<ByteArray, ByteArray> = emptyMap(),
    @Serializable(with = ByteArrayHexSerializer::class)
    val output: ByteArray
)
