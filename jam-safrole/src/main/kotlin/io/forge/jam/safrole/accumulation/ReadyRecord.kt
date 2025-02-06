package io.forge.jam.safrole.accumulation

import io.forge.jam.core.Encodable
import io.forge.jam.core.JamByteArray
import io.forge.jam.core.WorkReport
import io.forge.jam.core.encodeList
import io.forge.jam.core.serializers.JamByteArrayListHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class ReadyRecord(
    val report: WorkReport,
    @Serializable(with = JamByteArrayListHexSerializer::class)
    val dependencies: List<JamByteArray>,
) : Encodable {
    override fun encode(): ByteArray {
        return report.encode() + encodeList(dependencies)
    }
}
