package io.forge.jam.core

data class AssurancesExtrinsic(
    val assurances: List<AssuranceExtrinsic>
) {
    // Method to encode the extrinsic into a binary format
    fun encode(): ByteArray {
        // Version byte (assuming version 2 for the extrinsic)
        val versionByte = byteArrayOf(0x02.toByte())

        // Encode each assurance
        val encodedAssurances = assurances.map { it.encode() }

        // Concatenate all encoded assurances
        val concatenatedEncodedAssurances = encodedAssurances.reduce { acc, bytes -> acc + bytes }

        // Return the version byte followed by the concatenated encoded assurances
        return versionByte + concatenatedEncodedAssurances
    }
}
