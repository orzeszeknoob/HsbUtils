package com.orzeszek

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.orzeszek.config.HsbUtilsConfig
import com.orzeszek.render.WaypointRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import org.lwjgl.glfw.GLFW
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.*
import kotlin.math.sqrt
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter

class HsbUtils : ClientModInitializer {
    private var shouldExecuteCommand = false
    private var gemstoneType = ""
    private val gemstoneSymbols: MutableMap<String, String> = HashMap()
    private var lastLoadedGemstone: String? = ""
    private var mineshaftDetected = false
    private var mineshaftLeaveDetected = false
    private var lastMineshaftTime: Long = 0
    private val random = Random()
    private var currentWaypointIndex = -1
    private var currentWaypoints: List<Waypoint>? = null
    private var tickCounter = 0

    companion object {
        lateinit var config: HsbUtilsConfig
        private val KEYBIND_CONFIG = KeyBinding(
            "Gemstone Mod Config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "Gemstone Mod"
        )
        val PRISMATIC_PATTERN = Regex("PRISTINE! You found (.) Flawed (.+?) Gemstone x\\d+!.*").toPattern()
        val MINESHAFT_PATTERN = Regex("MINESHAFT!").toPattern()
        val MINESHAFT_LEAVE_PATTERN = Regex("BYE! You got a \\+\\d+,\\d+ Glacite Powder bonus for leaving a Mineshaft before you died!|☠ You froze to death.").toPattern()

        @JvmStatic var mineshaftDetected = false
        @JvmStatic var mineshaftLeaveDetected = false
        @JvmStatic var lastMineshaftTime: Long = 0
        @JvmStatic var gemstoneType = ""
        @JvmStatic var shouldExecuteCommand = false
        @JvmStatic var currentWaypoints: List<Waypoint>? = null
        @JvmStatic var lastLoadedGemstone: String? = ""
        @JvmStatic var currentWaypointIndex = -1

        fun addClientMessage(text: String) {
            MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.literal(text))
        }
    }

    override fun onInitializeClient() {
        config = HsbUtilsConfig()
        config.preload()
        
        // Register keybinding
        KeyBindingHelper.registerKeyBinding(KEYBIND_CONFIG)

        // Register commands
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            dispatcher.register(
                ClientCommandManager.literal("gemstone")
                    .then(ClientCommandManager.literal("waypoints")
                        .then(ClientCommandManager.literal("reset")
                            .executes { context ->
                                currentWaypoints = null
                                currentWaypointIndex = -1
                                lastLoadedGemstone = ""
                                addClientMessage("§eWaypoints reset")
                                1
                            }
                        )
                    )
            )
        }

        // Register event handlers
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            // Check for keybinding press
            if (KEYBIND_CONFIG.wasPressed()) {
                try {
                    client.setScreen(config.gui())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val currentTime = System.currentTimeMillis()
            val randomDelay = config.mineshaftDelay + random.nextFloat() * 250f

            if (config.enableHotmMacro && (mineshaftDetected || mineshaftLeaveDetected) && currentTime - lastMineshaftTime >= randomDelay) {
                if (mineshaftDetected) {
                    mineshaftDetected = false
                } else {
                    mineshaftLeaveDetected = false
                }
                client.player?.networkHandler?.sendCommand("hotm")
            }

            if (shouldExecuteCommand && client.currentScreen == null) {
                shouldExecuteCommand = false
                executeCommand()
            }
        }

        // Register waypoint renderer
        WorldRenderEvents.END.register { context: WorldRenderContext ->
            val matrixStack = context.matrixStack() ?: return@register
            val camera = context.camera()
            val tickDelta = context.tickCounter().fixedDeltaTicks
            WaypointRenderer.render(matrixStack, camera, tickDelta)
        }

        // Register HUD renderer for overlays and tickDelta-based animations
        HudRenderCallback.EVENT.register { drawContext: DrawContext, tickCounter: RenderTickCounter ->
            val tickDelta = tickCounter.fixedDeltaTicks
            WaypointRenderer.renderHud(drawContext, tickDelta)
        }

        gemstoneSymbols.apply {
            put("ruby", "\u2764")
            put("sapphire", "\u270E")
            put("jade", "\u2618")
            put("peridot", "\u2618")
            put("amber", "\u2E15")
            put("citrine", "\u2618")
            put("aquamarine", "\u2602")
            put("onyx", "\u2620")
            put("amethyst", "\u2748")
            put("topaz", "\u2727")
            put("jasper", "\u2741")
            put("opal", "\u25C2")
        }
    }

    private fun executeCommand() {
        if (gemstoneType == lastLoadedGemstone) return

        val file = File("config/hsbutils/$gemstoneType.txt")
        if (!file.exists()) return

        val waypoints = loadWaypointsFromFile(file)
        if (waypoints.isNullOrEmpty()) return

        currentWaypoints = waypoints
        lastLoadedGemstone = gemstoneType

        val content = file.readText().trim()
        val selection = StringSelection(content)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
    }

    data class Waypoint(val x: Double, val y: Double, val z: Double, val r: Float, val g: Float, val b: Float, val options: Options)
    data class Options(val name: Int)

    private val gson = Gson()

    fun loadWaypointsFromFile(file: File): List<Waypoint>? =
        if (!file.exists()) null else gson.fromJson(file.readText(), object : TypeToken<List<Waypoint>>() {}.type)

    fun parseWaypoints(json: String): List<Waypoint> = gson.fromJson(json, object : TypeToken<List<Waypoint>>() {}.type)

    fun distance(a: Vec3d, b: Vec3d): Double =
        sqrt((a.x - b.x).let { dx -> dx * dx } + (a.y - b.y).let { dy -> dy * dy } + (a.z - b.z).let { dz -> dz * dz })

    fun getPlayerPos(): Vec3d? = MinecraftClient.getInstance().player?.let { Vec3d(it.x, it.y, it.z) }

    fun findCurrentAndNextWaypoint(waypoints: List<Waypoint>, playerPos: Vec3d, threshold: Double = 3.5): Triple<Waypoint?, Waypoint?, Waypoint?> {
        var lastWaypoint: Waypoint? = null
        var currentWaypoint: Waypoint? = null
        var nextWaypoint: Waypoint? = null

        for (i in waypoints.indices) {
            val waypoint = waypoints[i]
            val waypointPos = Vec3d(waypoint.x, waypoint.y, waypoint.z)
            val dist = distance(playerPos, waypointPos)

            if (dist <= threshold) {
                currentWaypoint = waypoint
                if (i > 0) {
                    lastWaypoint = waypoints[i - 1]
                }
                if (i < waypoints.size - 1) {
                    nextWaypoint = waypoints[i + 1]
                }
                break
            }
        }

        if (currentWaypoint == null) {
            // Find the closest waypoint
            var minDist = Double.MAX_VALUE
            var closestIndex = -1

            for (i in waypoints.indices) {
                val waypoint = waypoints[i]
                val waypointPos = Vec3d(waypoint.x, waypoint.y, waypoint.z)
                val dist = distance(playerPos, waypointPos)

                if (dist < minDist) {
                    minDist = dist
                    closestIndex = i
                }
            }

            if (closestIndex >= 0) {
                currentWaypoint = waypoints[closestIndex]
                if (closestIndex > 0) {
                    lastWaypoint = waypoints[closestIndex - 1]
                }
                if (closestIndex < waypoints.size - 1) {
                    nextWaypoint = waypoints[closestIndex + 1]
                }
            }
        }

        return Triple(lastWaypoint, currentWaypoint, nextWaypoint)
    }
}