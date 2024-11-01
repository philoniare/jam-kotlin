package io.forge.jam.safrole

import io.forge.jam.core.serializers.ByteArrayListHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Psi(
    @SerialName("psi_g")
    @Serializable(with = ByteArrayListHexSerializer::class)
    val psiG: MutableList<ByteArray>,

    @SerialName("psi_b")
    @Serializable(with = ByteArrayListHexSerializer::class)
    val psiB: MutableList<ByteArray>,

    @SerialName("psi_w")
    @Serializable(with = ByteArrayListHexSerializer::class)
    val psiW: MutableList<ByteArray>,

    @SerialName("psi_o")
    @Serializable(with = ByteArrayListHexSerializer::class)
    val psiO: MutableList<ByteArray>,
)
