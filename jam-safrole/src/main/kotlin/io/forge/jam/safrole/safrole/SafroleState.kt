package io.forge.jam.safrole.safrole

import io.forge.jam.core.*
import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import io.forge.jam.core.serializers.JamByteArrayListHexSerializer
import io.forge.jam.safrole.*
import io.forge.jam.safrole.serializer.NullableAvailabilityAssignmentListSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Intermediate structure for decoded Safrole gamma state.
 */
data class SafroleGammaState(
    val gammaK: List<ValidatorKey>,
    val gammaA: List<TicketBody>,
    val gammaS: TicketsOrKeys,
    val gammaZ: JamByteArray
) {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0, validatorCount: Int, epochLength: Int): SafroleGammaState {
            var currentOffset = offset

            // γk: next epoch validators (validatorCount × 336 bytes)
            val gammaK = mutableListOf<ValidatorKey>()
            for (i in 0 until validatorCount) {
                if (currentOffset + ValidatorKey.SIZE <= data.size) {
                    gammaK.add(ValidatorKey.fromBytes(data, currentOffset))
                    currentOffset += ValidatorKey.SIZE
                }
            }

            // γz: ring root (144 bytes) - comes SECOND in Boka's order
            val gammaZ = if (currentOffset + 144 <= data.size) {
                JamByteArray(data.copyOfRange(currentOffset, currentOffset + 144))
            } else {
                JamByteArray(ByteArray(144))
            }
            currentOffset += 144

            // γs: sealing sequence (TicketsOrKeys) - comes THIRD
            val (gammaS, gammaSBytes) = TicketsOrKeys.fromBytes(data, currentOffset, epochLength)
            currentOffset += gammaSBytes

            // γa: ticket accumulator (variable length, compact-encoded) - comes LAST
            val (gammaALength, gammaALengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += gammaALengthBytes
            val gammaA = mutableListOf<TicketBody>()
            for (i in 0 until gammaALength.toInt()) {
                if (currentOffset + TicketBody.SIZE <= data.size) {
                    gammaA.add(TicketBody.fromBytes(data, currentOffset))
                    currentOffset += TicketBody.SIZE
                }
            }

            return SafroleGammaState(gammaK, gammaA, gammaS, gammaZ)
        }
    }
}

@Serializable
data class SafroleState(
    // Current timeslot
    var tau: Long,

    // Entropy accumulator
    val eta: JamByteArrayList = JamByteArrayList(),

    // Previous epoch validators
    var lambda: List<ValidatorKey> = emptyList(),

    // Current validators
    var kappa: List<ValidatorKey> = emptyList(),

    // Next epoch validators
    @SerialName("gamma_k")
    var gammaK: List<ValidatorKey> = emptyList(),

    // Queued validators
    val iota: List<ValidatorKey> = emptyList(),

    // Ticket accumulator
    @SerialName("gamma_a")
    var gammaA: List<TicketBody> = emptyList(),

    // Current sealing sequence
    @SerialName("gamma_s")
    var gammaS: TicketsOrKeys = TicketsOrKeys(),

    // Bandersnatch ring root
    @SerialName("gamma_z")
    @Serializable(with = JamByteArrayHexSerializer::class)
    var gammaZ: JamByteArray = JamByteArray(ByteArray(0)),

    @SerialName("post_offenders")
    @Serializable(with = JamByteArrayListHexSerializer::class)
    val postOffenders: List<JamByteArray>? = null,

    @Serializable(with = NullableAvailabilityAssignmentListSerializer::class)
    var rho: MutableList<AvailabilityAssignment?>? = null,
    var psi: Psi? = null
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0, validatorCount: Int, epochLength: Int): Pair<SafroleState, Int> {
            var currentOffset = offset

            // tau - 4 bytes
            val tau = decodeFixedWidthInteger(data, currentOffset, 4, false)
            currentOffset += 4

            // eta - 4 x 32-byte hashes (fixed size, no length prefix)
            val eta = JamByteArrayList()
            for (i in 0 until 4) {
                eta.add(JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32)))
                currentOffset += 32
            }

            // lambda - validatorCount validators (fixed array, no length prefix)
            val lambda = decodeFixedList(data, currentOffset, validatorCount, ValidatorKey.SIZE) { d, o ->
                ValidatorKey.fromBytes(d, o)
            }
            currentOffset += validatorCount * ValidatorKey.SIZE

            // kappa - validatorCount validators
            val kappa = decodeFixedList(data, currentOffset, validatorCount, ValidatorKey.SIZE) { d, o ->
                ValidatorKey.fromBytes(d, o)
            }
            currentOffset += validatorCount * ValidatorKey.SIZE

            // gammaK - validatorCount validators
            val gammaK = decodeFixedList(data, currentOffset, validatorCount, ValidatorKey.SIZE) { d, o ->
                ValidatorKey.fromBytes(d, o)
            }
            currentOffset += validatorCount * ValidatorKey.SIZE

            // iota - validatorCount validators
            val iota = decodeFixedList(data, currentOffset, validatorCount, ValidatorKey.SIZE) { d, o ->
                ValidatorKey.fromBytes(d, o)
            }
            currentOffset += validatorCount * ValidatorKey.SIZE

            // gammaA - compact length prefix + variable-size list
            val (gammaALength, gammaALengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += gammaALengthBytes
            val gammaA = decodeFixedList(data, currentOffset, gammaALength.toInt(), TicketBody.SIZE) { d, o ->
                TicketBody.fromBytes(d, o)
            }
            currentOffset += gammaALength.toInt() * TicketBody.SIZE

            // gammaS - TicketsOrKeys (Either)
            val (gammaS, gammaSBytes) = TicketsOrKeys.fromBytes(data, currentOffset, epochLength)
            currentOffset += gammaSBytes

            // gammaZ - 144 bytes (ring root)
            val gammaZ = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 144))
            currentOffset += 144

            // postOffenders - compact length prefix + 32-byte hashes
            val (postOffendersLength, postOffendersLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += postOffendersLengthBytes
            val postOffenders = if (postOffendersLength > 0) {
                val list = mutableListOf<JamByteArray>()
                for (i in 0 until postOffendersLength.toInt()) {
                    list.add(JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32)))
                    currentOffset += 32
                }
                list
            } else null

            return Pair(
                SafroleState(
                    tau, eta, lambda, kappa, gammaK, iota, gammaA, gammaS, gammaZ, postOffenders
                ),
                currentOffset - offset
            )
        }
    }
    fun deepCopy(): SafroleState {
        return SafroleState(
            tau = tau,
            eta = eta.clone(),
            lambda = lambda.map { it.copy() },
            kappa = kappa.map { it.copy() },
            gammaK = gammaK.map { it.copy() },
            iota = iota.map { it.copy() },
            gammaA = gammaA.map { it.copy() },
            gammaS = gammaS.copy(),
            gammaZ = gammaZ.clone(),
            postOffenders = postOffenders?.map { it.copy() },
            rho = rho?.map { it?.copy() }?.toMutableList(),
            psi = psi?.copy()
        )
    }

    override fun encode(): ByteArray {
        val tauBytes = encodeFixedWidthInteger(tau, 4, false)
        val etaBytes = encodeList(eta.toList(), false)
        val lambdaBytes = encodeList(lambda, false)
        val kappaBytes = encodeList(kappa, false)
        val gammaKBytes = encodeList(gammaK, false)
        val iotaBytes = encodeList(iota, false)
        val gammaABytes = encodeListWithCompactLength(gammaA)
        val gammaSBytes = gammaS.encode()
        val gammaZBytes = gammaZ.bytes
        val postOffendersBytes = postOffenders?.let { encodeListWithCompactLength(it) } ?: byteArrayOf(0)
        return tauBytes + etaBytes + lambdaBytes + kappaBytes + gammaKBytes + iotaBytes + gammaABytes + gammaSBytes + gammaZBytes + postOffendersBytes
    }
}
