package com.orzeszek.config

import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import java.io.File
import com.google.gson.annotations.Expose

class HsbUtilsConfig {
    @Expose
    var enableHotmMacro = false
    @Expose
    var mineshaftDelay = 500
    @Expose
    var showWaypoints = true
    @Expose
    var showWaypointLabels = true
    @Expose
    var showWaypointLines = true

    private val configFile = File("config/hsbutils/config.json")

    fun preload() {
        if (!configFile.exists()) {
            configFile.parentFile.mkdirs()
            save()
        } else {
            load()
        }
    }

    fun save() {
        val gson = com.google.gson.GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setLenient()
            .create()
        configFile.writeText(gson.toJson(this))
    }

    fun load() {
        try {
            val gson = com.google.gson.GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setLenient()
                .create()
            val loaded = gson.fromJson(configFile.readText(), HsbUtilsConfig::class.java)
            enableHotmMacro = loaded.enableHotmMacro
            mineshaftDelay = loaded.mineshaftDelay
            showWaypoints = loaded.showWaypoints
            showWaypointLabels = loaded.showWaypointLabels
            showWaypointLines = loaded.showWaypointLines
        } catch (e: Exception) {
            e.printStackTrace()
            enableHotmMacro = false
            mineshaftDelay = 500
            showWaypoints = true
            showWaypointLabels = true
            showWaypointLines = true
            save()
        }
    }

    fun gui(): Screen {
        val builder = ConfigBuilder.create()
            .setTitle(Text.literal("HSB Utils Config"))
            .setSavingRunnable { save() }
            .setDefaultBackgroundTexture(net.minecraft.util.Identifier.of("minecraft", "textures/block/stone.png"))

        val mineshaftCategory = builder.getOrCreateCategory(Text.literal("Mineshaft Mining"))
        val waypointsCategory = builder.getOrCreateCategory(Text.literal("Waypoints"))
        val entryBuilder = builder.entryBuilder

        mineshaftCategory.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable HOTM Macro"), enableHotmMacro)
            .setDefaultValue(false)
            .setTooltip(Text.literal("Enable automatic HOTM command execution when entering/leaving mineshafts (Use at your own risk)"))
            .setSaveConsumer { enableHotmMacro = it }
            .build())

        mineshaftCategory.addEntry(entryBuilder.startIntSlider(Text.literal("Mineshaft Delay"), mineshaftDelay, 500, 1000)
            .setDefaultValue(500)
            .setTooltip(Text.literal("Delay in milliseconds before executing mineshaft commands"))
            .setSaveConsumer { mineshaftDelay = it }
            .build())

        waypointsCategory.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Waypoints"), showWaypoints)
            .setDefaultValue(true)
            .setTooltip(Text.literal("Show waypoints in the world"))
            .setSaveConsumer { showWaypoints = it }
            .build())

        waypointsCategory.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Waypoint Labels"), showWaypointLabels)
            .setDefaultValue(true)
            .setTooltip(Text.literal("Show labels on waypoints"))
            .setSaveConsumer { showWaypointLabels = it }
            .build())

        waypointsCategory.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Waypoint Lines"), showWaypointLines)
            .setDefaultValue(true)
            .setTooltip(Text.literal("Show lines connecting waypoints"))
            .setSaveConsumer { showWaypointLines = it }
            .build())

        return builder.build()
    }
} 