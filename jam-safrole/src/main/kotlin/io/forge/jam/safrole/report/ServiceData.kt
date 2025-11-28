package io.forge.jam.safrole.report

import io.forge.jam.core.Encodable
import io.forge.jam.core.JamByteArray
import io.forge.jam.core.encodeList
import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import io.forge.jam.safrole.preimage.PreimageHash
import kotlinx.serialization.Serializable

@Serializable
data class StorageMapEntry(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val key: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val value: JamByteArray
) : Encodable {
    override fun encode(): ByteArray {
        return key.bytes + value.bytes
    }
}

@Serializable
class ServiceData(
    val service: ServiceInfo,
    val storage: List<StorageMapEntry> = emptyList(),
    val preimages: List<PreimageHash> = emptyList()
) : Encodable {
    override fun encode(): ByteArray {
        val storageBytes = encodeList(storage)
        val preimageBytes = encodeList(preimages)
        return service.encode() + storageBytes + preimageBytes
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ServiceData) return false

        return service == other.service && storage == other.storage && preimages == other.preimages
    }

    override fun hashCode(): Int {
        var result = service.hashCode()
        result = 31 * result + storage.hashCode()
        result = 31 * result + preimages.hashCode()
        return result
    }
}
