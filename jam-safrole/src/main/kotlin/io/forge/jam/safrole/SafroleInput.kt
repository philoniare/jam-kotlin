package io.forge.jam.safrole

import io.forge.jam.core.TicketEnvelope
import io.forge.jam.core.serializers.ByteArrayHexSerializer
import io.forge.jam.core.serializers.ByteArrayListHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SafroleInput(
    val slot: Long,
    @Serializable(with = ByteArrayHexSerializer::class)
    val entropy: ByteArray,
    val extrinsic: List<TicketEnvelope>,

    @SerialName("post_offenders")
    @Serializable(with = ByteArrayListHexSerializer::class)
    val postOffenders: List<ByteArray>,
)
