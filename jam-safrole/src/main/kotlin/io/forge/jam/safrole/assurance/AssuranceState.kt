package io.forge.jam.safrole.assurance

import io.forge.jam.core.Encodable
import io.forge.jam.core.WorkReport
import io.forge.jam.core.decodeFixedWidthInteger
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
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0, coresCount: Int, validatorsCount: Int): Pair<AssuranceState, Int> {
            var currentOffset = offset

            // availAssignments - fixed size list (coresCount) of optional AvailabilityAssignment
            val availAssignments = mutableListOf<AvailabilityAssignment?>()
            for (i in 0 until coresCount) {
                // Each optional has a discriminator byte (0 = None, 1 = Some)
                val discriminator = data[currentOffset].toInt() and 0xFF
                currentOffset += 1

                if (discriminator == 0) {
                    availAssignments.add(null)
                } else {
                    val (assignment, assignmentBytes) = AvailabilityAssignment.fromBytes(data, currentOffset)
                    availAssignments.add(assignment)
                    currentOffset += assignmentBytes
                }
            }

            // currValidators - fixed size list (validatorsCount)
            val currValidators = mutableListOf<ValidatorKey>()
            for (i in 0 until validatorsCount) {
                currValidators.add(ValidatorKey.fromBytes(data, currentOffset))
                currentOffset += ValidatorKey.SIZE
            }

            return Pair(AssuranceState(availAssignments, currValidators), currentOffset - offset)
        }
    }

    override fun encode(): ByteArray {
        // AvailabilityAssignments has fixed size (core-count), so no length prefix
        // ValidatorsData has fixed size (validators-count), so no length prefix
        return encodeOptionalList(availAssignments, includeLength = false) + encodeList(currValidators, includeLength = false)
    }
}
