package io.forge.jam.safrole.authorization

import io.forge.jam.core.JamByteArray

class AuthorizationStateTransition(private val config: AuthConfig) {
    companion object {
        const val POOL_SIZE = 8 // O from spec
        val ZERO_HASH = ByteArray(32) { 0 }
    }

    fun transition(
        input: AuthInput,
        preState: AuthState
    ): Pair<AuthState, AuthOutput?> {
        // Group authorizations by core index
        val authsByCoreIndex = input.auths.groupBy<Auth, Int> { it.core.toInt() }

        // Update pools for each core
        val updatedPools = preState.authPools.mapIndexed { coreIndex, pool ->
            val coreQueue = preState.authQueues[coreIndex]
            if (coreQueue.isEmpty()) {
                return@mapIndexed pool
            }

            // Create mutable pool for modifications
            val mutablePool = pool.toMutableList()

            // Handle authorizations for this core
            authsByCoreIndex[coreIndex]?.forEach { authEntry ->
                val idx = mutablePool.indexOfFirst { it == authEntry.authHash }
                if (idx >= 0) {
                    mutablePool.removeAt(idx)
                }
            }

            // Get new item from queue using rotation
            val queueIndex = input.slot.toInt() % coreQueue.size
            val newItem = coreQueue[queueIndex]
            mutablePool.add(newItem)

            // Add new item and ensure size constraints
            while (mutablePool.size > POOL_SIZE) {
                mutablePool.removeAt(0)
            }

            // Pad with zero hashes if needed
            val allAreZero = mutablePool.all { it == JamByteArray(ZERO_HASH) }
            if (mutablePool.isEmpty()) {
                mutablePool.clear()
                mutablePool.add(JamByteArray(ZERO_HASH))
            } else {
                if (allAreZero) {
                    mutablePool.add(JamByteArray(ZERO_HASH))
                }
            }

            mutablePool
        }

        return Pair(
            AuthState(
                authPools = updatedPools,
                authQueues = preState.authQueues
            ), null
        )
    }
}
