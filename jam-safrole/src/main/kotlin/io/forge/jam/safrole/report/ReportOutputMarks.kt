package io.forge.jam.safrole.report

import io.forge.jam.core.serializers.ByteArrayListHexSerializer
import io.forge.jam.core.serializers.ByteArrayNestedListSerializer
import kotlinx.serialization.Serializable

@Serializable
data class ReportOutputMarks(
    @Serializable(with = ByteArrayNestedListSerializer::class)
    val reported: List<List<ByteArray>>,
    @Serializable(with = ByteArrayListHexSerializer::class)
    val reporters: List<ByteArray>
)
