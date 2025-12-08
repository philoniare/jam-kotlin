package io.forge.jam.pvm.program

/**
 * Represents the instruction opcodes for the virtual machine.
 */
@Suppress("EnumEntryName")
enum class Opcode(val value: UByte) {
    panic(0u),
    fallthrough(1u),
    memset(2u),
    jump_indirect(50u),
    load_imm(51u),
    load_u8(52u),
    load_i8(53u),
    load_u16(54u),
    load_i16(55u),
    load_i32(57u),
    load_u32(56u),
    load_u64(58u),
    store_u8(59u),
    store_u16(60u),
    store_u32(61u),
    store_u64(62u),
    load_imm_and_jump(80u),
    branch_eq_imm(81u),
    branch_not_eq_imm(82u),
    branch_less_unsigned_imm(83u),
    branch_less_signed_imm(87u),
    branch_greater_or_equal_unsigned_imm(85u),
    branch_greater_or_equal_signed_imm(89u),
    branch_less_or_equal_signed_imm(88u),
    branch_less_or_equal_unsigned_imm(84u),
    branch_greater_signed_imm(90u),
    branch_greater_unsigned_imm(86u),
    store_imm_indirect_u8(70u),
    store_imm_indirect_u16(71u),
    store_imm_indirect_u32(72u),
    store_imm_indirect_u64(73u),
    store_indirect_u8(120u),
    store_indirect_u16(121u),
    store_indirect_u32(122u),
    store_indirect_u64(123u),
    load_indirect_u8(124u),
    load_indirect_i8(125u),
    load_indirect_u16(126u),
    load_indirect_i16(127u),
    load_indirect_i32(129u),
    load_indirect_u32(128u),
    load_indirect_u64(130u),
    add_imm_32(131u),
    add_imm_64(149u),
    and_imm(132u),
    xor_imm(133u),
    or_imm(134u),
    mul_imm_32(135u),
    mul_imm_64(150u),
    set_less_than_unsigned_imm(136u),
    set_less_than_signed_imm(137u),
    shift_logical_left_imm_32(138u),
    shift_logical_left_imm_64(151u),
    shift_logical_right_imm_32(139u),
    shift_logical_right_imm_64(152u),
    shift_arithmetic_right_imm_32(140u),
    shift_arithmetic_right_imm_64(153u),
    negate_and_add_imm_32(141u),
    negate_and_add_imm_64(154u),
    set_greater_than_unsigned_imm(142u),
    set_greater_than_signed_imm(143u),
    shift_logical_right_imm_alt_32(145u),
    shift_logical_right_imm_alt_64(156u),
    shift_arithmetic_right_imm_alt_32(146u),
    shift_arithmetic_right_imm_alt_64(157u),
    shift_logical_left_imm_alt_32(144u),
    shift_logical_left_imm_alt_64(155u),
    cmov_if_zero_imm(147u),
    cmov_if_not_zero_imm(148u),
    rotate_right_imm_32(160u),
    rotate_right_imm_alt_32(161u),
    rotate_right_imm_64(158u),
    rotate_right_imm_alt_64(159u),
    branch_eq(170u),
    branch_not_eq(171u),
    branch_less_unsigned(172u),
    branch_less_signed(173u),
    branch_greater_or_equal_unsigned(174u),
    branch_greater_or_equal_signed(175u),
    add_32(190u),
    add_64(200u),
    sub_32(191u),
    sub_64(201u),
    and(210u),
    xor(211u),
    or(212u),
    mul_32(192u),
    mul_64(202u),
    mul_upper_signed_signed(213u),
    mul_upper_unsigned_unsigned(214u),
    mul_upper_signed_unsigned(215u),
    set_less_than_unsigned(216u),
    set_less_than_signed(217u),
    shift_logical_left_32(197u),
    shift_logical_left_64(207u),
    shift_logical_right_32(198u),
    shift_logical_right_64(208u),
    shift_arithmetic_right_32(199u),
    shift_arithmetic_right_64(209u),
    div_unsigned_32(193u),
    div_unsigned_64(203u),
    div_signed_32(194u),
    div_signed_64(204u),
    rem_unsigned_32(195u),
    rem_unsigned_64(205u),
    rem_signed_32(196u),
    rem_signed_64(206u),
    cmov_if_zero(218u),
    cmov_if_not_zero(219u),
    and_inverted(224u),
    or_inverted(225u),
    xnor(226u),
    maximum(227u),
    maximum_unsigned(228u),
    minimum(229u),
    minimum_unsigned(230u),
    rotate_left_32(221u),
    rotate_left_64(220u),
    rotate_right_32(223u),
    rotate_right_64(222u),
    jump(40u),
    ecalli(10u),
    store_imm_u8(30u),
    store_imm_u16(31u),
    store_imm_u32(32u),
    store_imm_u64(33u),
    move_reg(100u),
    sbrk(101u),
    count_leading_zero_bits_32(105u),
    count_leading_zero_bits_64(104u),
    count_trailing_zero_bits_32(107u),
    count_trailing_zero_bits_64(106u),
    count_set_bits_32(103u),
    count_set_bits_64(102u),
    sign_extend_8(108u),
    sign_extend_16(109u),
    zero_extend_16(110u),
    reverse_byte(111u),
    load_imm_and_jump_indirect(180u),
    load_imm_64(20u);


    companion object {
        /**
         * Lookup table mapping byte values to Opcode instances
         */
        private val lookupTable: Map<UByte, Opcode> = entries.associateBy { it.value }

        /**
         * Creates an Opcode from any byte value.
         *
         * @param byte The byte value to convert
         * @return The corresponding Opcode or null if the byte value is invalid
         */
        fun fromUByteAny(byte: UByte): Opcode? = lookupTable[byte]


    }

    override fun toString(): String = name

    fun canFallthrough(): Boolean = when (this) {
        panic,
        jump,
        jump_indirect,
        load_imm_and_jump,
        load_imm_and_jump_indirect -> false

        else -> true
    }

    fun startsNewBasicBlock(): Boolean = when (this) {
        panic,
        fallthrough,
        jump,
        jump_indirect,
        load_imm_and_jump,
        load_imm_and_jump_indirect,
        branch_eq,
        branch_eq_imm,
        branch_greater_or_equal_signed,
        branch_greater_or_equal_signed_imm,
        branch_greater_or_equal_unsigned,
        branch_greater_or_equal_unsigned_imm,
        branch_greater_signed_imm,
        branch_greater_unsigned_imm,
        branch_less_or_equal_signed_imm,
        branch_less_or_equal_unsigned_imm,
        branch_less_signed,
        branch_less_signed_imm,
        branch_less_unsigned,
        branch_less_unsigned_imm,
        branch_not_eq,
        branch_not_eq_imm -> true

        else -> false
    }
}
