package io.forge.jam.safrole.safrole

data class SafroleConfig(
    val validatorsCount: Int,
    val coresCount: Int,
    val epochLength: Long,
    val ticketCutoff: Long,
    val ringSize: Int,
    val maxTicketAttempts: Int,
) {
    // Compute thresholds based on configured validator count
    val superMajority: Int = (2 * validatorsCount / 3) + 1
    val oneThird: Int = validatorsCount / 3

    // Set of valid vote thresholds
    val validVoteThresholds = setOf(0, oneThird, superMajority)
}
