package io.forge.jam.safrole

import io.forge.jam.core.TicketEnvelope
import io.forge.jam.core.serializers.ByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class SafroleInput(
    val slot: Long,
    @Serializable(with = ByteArrayHexSerializer::class)
    val entropy: ByteArray,
    val extrinsic: List<TicketEnvelope>
)
