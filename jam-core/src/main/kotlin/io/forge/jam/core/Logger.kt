package io.forge.jam.core

/**
 * Logger interface for the Jam library
 */
private interface Logger {
    fun trace(message: () -> String)
}

private val logger = object : Logger {
    override fun trace(message: () -> String) {
        println("[TRACE] ${message()}")
    }
}
