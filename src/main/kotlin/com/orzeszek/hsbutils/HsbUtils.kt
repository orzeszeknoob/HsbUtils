package com.orzeszek.hsbutils

import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

class HsbUtils : ClientModInitializer {
    companion object {
        const val MOD_ID = "hsbutils"
        private val LOGGER = LoggerFactory.getLogger(MOD_ID)
    }

    override fun onInitializeClient() {
        LOGGER.info("Initializing HSB Utils (client)")
        // Client-only initialization here
    }
} 