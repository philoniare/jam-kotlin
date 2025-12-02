package io.forge.jam.safrole.safrole

import io.forge.jam.core.Encodable
import io.forge.jam.core.SafroleErrorCode
import io.forge.jam.core.SafroleErrorCodeSerializer
import io.forge.jam.safrole.OutputMarks
import kotlinx.serialization.Serializable

@Serializable
data class SafroleOutput(
    val ok: OutputMarks? = null,
    @Serializable(with = SafroleErrorCodeSerializer::class)
    val err: SafroleErrorCode? = null
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0, validatorCount: Int, epochLength: Int): Pair<SafroleOutput, Int> {
            var currentOffset = offset

            // discriminator - 0 = ok, 1 = err
            val discriminator = data[currentOffset].toInt() and 0xFF
            currentOffset += 1

            return if (discriminator == 0) {
                val (marks, marksBytes) = OutputMarks.fromBytes(data, currentOffset, validatorCount, epochLength)
                currentOffset += marksBytes
                Pair(SafroleOutput(ok = marks, err = null), currentOffset - offset)
            } else {
                val errorOrdinal = data[currentOffset].toInt() and 0xFF
                currentOffset += 1
                val error = SafroleErrorCode.entries[errorOrdinal]
                Pair(SafroleOutput(ok = null, err = error), currentOffset - offset)
            }
        }
    }

    override fun encode(): ByteArray {
        return if (ok != null) {
            byteArrayOf(0) + ok.encode()
        } else if (err != null) {
            byteArrayOf(1) + err!!.encode()
        } else {
            byteArrayOf(0)
        }
    }
}
