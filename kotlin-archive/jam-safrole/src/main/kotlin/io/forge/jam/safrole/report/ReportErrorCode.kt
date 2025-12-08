package io.forge.jam.safrole.report

import io.forge.jam.core.Encodable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ReportErrorCode(val value: Int) : Encodable {
    @SerialName("bad_core_index")
    BAD_CORE_INDEX(0),

    @SerialName("future_report_slot")
    FUTURE_REPORT_SLOT(1),

    @SerialName("report_epoch_before_last")
    REPORT_EPOCH_BEFORE_LAST(2),

    @SerialName("insufficient_guarantees")
    INSUFFICIENT_GUARANTEES(3),

    @SerialName("out_of_order_guarantee")
    OUT_OF_ORDER_GUARANTEE(4),

    @SerialName("not_sorted_or_unique_guarantors")
    NOT_SORTED_OR_UNIQUE_GUARANTORS(5),

    @SerialName("wrong_assignment")
    WRONG_ASSIGNMENT(6),

    @SerialName("core_engaged")
    CORE_ENGAGED(7),

    @SerialName("anchor_not_recent")
    ANCHOR_NOT_RECENT(8),

    @SerialName("bad_service_id")
    BAD_SERVICE_ID(9),

    @SerialName("bad_code_hash")
    BAD_CODE_HASH(10),

    @SerialName("dependency_missing")
    DEPENDENCY_MISSING(11),

    @SerialName("duplicate_package")
    DUPLICATE_PACKAGE(12),

    @SerialName("bad_state_root")
    BAD_STATE_ROOT(13),

    @SerialName("bad_beefy_mmr_root")
    BAD_BEEFY_MMR_ROOT(14),

    @SerialName("core_unauthorized")
    CORE_UNAUTHORIZED(15),

    @SerialName("bad_validator_index")
    BAD_VALIDATOR_INDEX(16),

    @SerialName("work_report_gas_too_high")
    WORK_REPORT_GAS_TOO_HIGH(17),

    @SerialName("service_item_gas_too_low")
    SERVICE_ITEM_GAS_TOO_LOW(18),

    @SerialName("too_many_dependencies")
    TOO_MANY_DEPENDENCIES(19),

    @SerialName("segment_root_lookup_invalid")
    SEGMENT_ROOT_LOOKUP_INVALID(20),

    @SerialName("bad_signature")
    BAD_SIGNATURE(21),

    @SerialName("work_report_too_big")
    WORK_REPORT_TOO_BIG(22),

    @SerialName("banned_validator")
    BANNED_VALIDATOR(23),

    @SerialName("lookup_anchor_not_recent")
    LOOKUP_ANCHOR_NOT_RECENT(24),

    @SerialName("missing_work_results")
    MISSING_WORK_RESULTS(25),

    @SerialName("duplicate_guarantors")
    DUPLICATE_GUARANTORS(26); // Not in ASN, but used in code

    override fun encode(): ByteArray {
        return byteArrayOf(value.toByte())
    }
}
