package io.forge.jam.safrole.preimage

import io.forge.jam.core.Encodable
import io.forge.jam.core.encodeList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountInfo(
    @SerialName("preimage_blobs")
    var preimages: List<PreimageHash>,
    @SerialName("preimage_requests")
    var lookupMeta: List<PreimageHistory>,
) : Encodable {
    override fun encode(): ByteArray {
        return encodeList(preimages) + encodeList(lookupMeta)
    }
}
