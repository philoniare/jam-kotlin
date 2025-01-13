package io.forge.jam.safrole.preimage

import io.forge.jam.core.Encodable
import io.forge.jam.core.encodeList
import kotlinx.serialization.Serializable

@Serializable
data class AccountInfo(
    var preimages: List<PreimageHash>,
    var history: List<PreimageHistory>,
) : Encodable {
    override fun encode(): ByteArray {
        return encodeList(preimages)
    }
}
