package io.forge.jam.safrole.assurance

import io.forge.jam.core.Encodable
import kotlinx.serialization.Serializable

@Serializable
data class AssuranceOutput(
    val ok: AssuranceOutputMarks? = null,
    @Serializable(with = AssuranceErrorSerializer::class)
    val err: AssuranceErrorCode? = null
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<AssuranceOutput, Int> {
            var currentOffset = offset

            // discriminator - 0 = ok, 1 = err
            val discriminator = data[currentOffset].toInt() and 0xFF
            currentOffset += 1

            return if (discriminator == 0) {
                val (marks, marksBytes) = AssuranceOutputMarks.fromBytes(data, currentOffset)
                currentOffset += marksBytes
                Pair(AssuranceOutput(ok = marks, err = null), currentOffset - offset)
            } else {
                val errorOrdinal = data[currentOffset].toInt() and 0xFF
                currentOffset += 1
                val error = AssuranceErrorCode.entries[errorOrdinal]
                Pair(AssuranceOutput(ok = null, err = error), currentOffset - offset)
            }
        }
    }

    override fun encode(): ByteArray {
        return if (ok != null) {
            // Prepend a 0 byte to indicate "ok" choice
            byteArrayOf(0) + ok.encode()
        } else {
            // Prepend a 1 byte to indicate "err" choice
            byteArrayOf(1) + err!!.encode()
        }
    }
}
