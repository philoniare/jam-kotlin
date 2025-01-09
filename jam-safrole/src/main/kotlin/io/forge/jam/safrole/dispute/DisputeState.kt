package io.forge.jam.safrole.safrole

import io.forge.jam.core.Encodable
import io.forge.jam.core.encodeFixedWidthInteger
import io.forge.jam.core.encodeList
import io.forge.jam.core.encodeOptionalList
import io.forge.jam.safrole.AvailabilityAssignment
import io.forge.jam.safrole.Psi
import io.forge.jam.safrole.ValidatorKey
import io.forge.jam.safrole.serializer.NullableAvailabilityAssignmentListSerializer
import kotlinx.serialization.Serializable

@Serializable
data class DisputeState(
    var psi: Psi? = null,
    @Serializable(with = NullableAvailabilityAssignmentListSerializer::class)
    var rho: List<AvailabilityAssignment?> = emptyList(),
    // Current timeslot
    var tau: Long,

    // Current validators
    var kappa: List<ValidatorKey> = emptyList(),
    // Previous epoch validators
    var lambda: List<ValidatorKey> = emptyList(),

    ) : Encodable {
    fun deepCopy(): SafroleState {
        return SafroleState(
            psi = psi?.copy(),
            rho = rho.map { it?.copy() }.toMutableList(),
            tau = tau,
            kappa = kappa.map { it.copy() },
            lambda = lambda.map { it.copy() },
        )
    }

    override fun encode(): ByteArray {
        val psiBytes = psi?.encode() ?: byteArrayOf(0)
        val rhoBytes = encodeOptionalList(rho, false)
        val tauBytes = encodeFixedWidthInteger(tau, 4, false)
        val kappaBytes = encodeList(kappa, false)
        val lambdaBytes = encodeList(lambda, false)
        return psiBytes + rhoBytes + tauBytes + kappaBytes + lambdaBytes
    }
}
