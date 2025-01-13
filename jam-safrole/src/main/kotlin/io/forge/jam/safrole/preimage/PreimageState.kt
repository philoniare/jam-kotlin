package io.forge.jam.safrole.preimage

import io.forge.jam.core.Encodable
import io.forge.jam.core.encodeList
import kotlinx.serialization.Serializable

@Serializable
data class PreimageState(
    var accounts: List<PreimageAccount>,
) : Encodable {
    override fun encode(): ByteArray {
        return encodeList(accounts)
    }
}
