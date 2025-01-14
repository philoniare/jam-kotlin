package io.forge.jam.safrole.assurance

import io.forge.jam.core.Encodable
import io.forge.jam.core.WorkReport
import io.forge.jam.core.encodeList
import kotlinx.serialization.Serializable

@Serializable
data class AssuranceOutputMarks(
    val reported: List<WorkReport>,
) : Encodable {
    override fun encode(): ByteArray {
        return encodeList(reported)
    }
}
