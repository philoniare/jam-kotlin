package io.forge.jam.safrole.assurance

import io.forge.jam.core.Encodable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AssuranceErrorCode : Encodable {
    @SerialName("bad_attestation_parent")
    BAD_ATTESTATION_PARENT,

    @SerialName("bad_validator_index")
    BAD_VALIDATOR_INDEX,

    @SerialName("core_not_engaged")
    CORE_NOT_ENGAGED,

    @SerialName("bad_signature")
    BAD_SIGNATURE,

    @SerialName("not_sorted_or_unique_assurers")
    NOT_SORTED_OR_UNIQUE_ASSURERS;

    override fun encode(): ByteArray {
        return byteArrayOf(ordinal.toByte())
    }
}

