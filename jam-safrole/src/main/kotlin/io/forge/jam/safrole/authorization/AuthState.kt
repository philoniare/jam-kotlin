package io.forge.jam.safrole.authorization

import io.forge.jam.core.Encodable
import io.forge.jam.core.JamByteArray
import io.forge.jam.core.encodeNestedList
import io.forge.jam.core.serializers.ByteArrayNestedListSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthState(
    @SerialName("auth_pools")
    @Serializable(with = ByteArrayNestedListSerializer::class)
    var authPools: List<List<JamByteArray>>,
    @SerialName("auth_queues")
    @Serializable(with = ByteArrayNestedListSerializer::class)
    val authQueues: List<List<JamByteArray>>
) : Encodable {
    fun copy() = AuthState(
        authPools = authPools.map { it.toList() },
        authQueues = authQueues.map { it.toList() }
    )

    override fun encode(): ByteArray {
        return encodeNestedList(authPools) + encodeNestedList(authQueues)
    }
}
