package io.forge.jam.safrole.assurance

import io.forge.jam.core.Encodable
import io.forge.jam.core.encodeList
import io.forge.jam.core.encodeOptionalList
import io.forge.jam.safrole.AvailabilityAssignment
import io.forge.jam.safrole.ValidatorKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssuranceState(
    @SerialName("avail_assignments")
    var availAssignments: List<AvailabilityAssignment?>,
    @SerialName("curr_validators")
    val currValidators: List<ValidatorKey>,
) : Encodable {
    override fun encode(): ByteArray {
        return encodeOptionalList(availAssignments) + encodeList(currValidators)
    }
}
