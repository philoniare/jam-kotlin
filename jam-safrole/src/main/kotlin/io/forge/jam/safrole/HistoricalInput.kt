package io.forge.jam.safrole

import io.forge.jam.core.serializers.ByteArrayHexSerializer
import io.forge.jam.core.serializers.ByteArrayListHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoricalInput(
    @SerialName("header_hash")
    @Serializable(with = ByteArrayHexSerializer::class)
    val headerHash: ByteArray,

    @SerialName("parent_state_root")
    @Serializable(with = ByteArrayHexSerializer::class)
    val parentStateRoot: ByteArray,

    @SerialName("accumulate_root")
    @Serializable(with = ByteArrayHexSerializer::class)
    val accumulateRoot: ByteArray,

    @SerialName("work_packages")
    @Serializable(with = ByteArrayListHexSerializer::class)
    val workPackages: List<ByteArray>
)
