package io.forge.jam.core

import io.forge.jam.core.serializers.ByteArrayHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportedWorkPackage(
    @Serializable(with = ByteArrayHexSerializer::class)
    val hash: ByteArray,
    @SerialName("exports_root")
    @Serializable(with = ByteArrayHexSerializer::class)
    val exportsRoot: ByteArray
) : Encodable {
    override fun encode(): ByteArray {
        return hash + exportsRoot
    }
}
