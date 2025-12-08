package io.forge.jam.pvm

import org.slf4j.LoggerFactory

class PvmLogger(clazz: Class<*>) {
    private val logger = LoggerFactory.getLogger(clazz)

    fun debug(message: String) {
        if (logger.isDebugEnabled) {
            logger.debug(message, "book")
        }
    }
}
