package com.orzeszek

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.orzeszek.config.HsbUtilsConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.*
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.*
import java.util.*
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

@Mod(modid = "hsbutils", version = "1.0", name = "HSButils", modLanguage = "kotlin", clientSideOnly = true)
class HsbUtils {
    private var shouldExecuteCommand = false
    private var gemstoneType = ""
    private val gemstoneSymbols: MutableMap<String?, String?> = HashMap()
    private var lastLoadedGemstone: String? = ""
    private var mineshaftDetected = false
    private var mineshaftLeaveDetected = false
    private var lastMineshaftTime: Long = 0
    private val random = Random()
    private var currentWaypointIndex = -1
    private var currentWaypoints: List<Waypoint>? = null
    private var tickCounter = 0
    private val notifiedPlayers = mutableSetOf<String>()
    private var lastWorldId: Int = -1

    companion object {
        lateinit var config: HsbUtilsConfig
        private val KEYBIND_CONFIG = KeyBinding("Gemstone Mod Config", Keyboard.KEY_O, "Gemstone Mod")
        private val PRISMATIC_PATTERN = Regex("PRISTINE! You found (.) Flawed (.+?) Gemstone x\\d+!.*").toPattern()
        private val MINESHAFT_PATTERN = Regex("MINESHAFT!").toPattern()
        private val MINESHAFT_LEAVE_PATTERN = Regex("BYE! You got a \\+\\d+,\\d+ Glacite Powder bonus for leaving a Mineshaft before you died!|☠ You froze to death.").toPattern()

        fun addClientMessage(text: String) {
            Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessage(ChatComponentText(text))
        }
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent?) {
        config = HsbUtilsConfig()
        config.preload()
        MinecraftForge.EVENT_BUS.register(this)
        MinecraftForge.EVENT_BUS.register(config)
        ClientCommandHandler.instance.registerCommand(CommandWaypoints())
        ClientRegistry.registerKeyBinding(KEYBIND_CONFIG)

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

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        val message = event.message.unformattedText

        if (MINESHAFT_PATTERN.matcher(message).find()) {
            mineshaftDetected = true
            lastMineshaftTime = System.currentTimeMillis()
            return
        }

        if (MINESHAFT_LEAVE_PATTERN.matcher(message).find()) {
            mineshaftLeaveDetected = true
            lastMineshaftTime = System.currentTimeMillis()
            return
        }

        val matcher = PRISMATIC_PATTERN.matcher(message)
        if (matcher.find()) {
            val gemstone = matcher.group(2).lowercase(Locale.getDefault()).trim()
            val file = File("config/hsbutils/$gemstone.txt")
            if (file.exists()) {
                gemstoneType = gemstone
                shouldExecuteCommand = true
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent?) {
        val currentTime = System.currentTimeMillis()
        val randomDelay = config.mineshaftDelay + random.nextFloat() * 250f

        if (config.enableHotmMacro && (mineshaftDetected || mineshaftLeaveDetected) && currentTime - lastMineshaftTime >= randomDelay) {
            if (mineshaftDetected) {
                mineshaftDetected = false
            } else {
                mineshaftLeaveDetected = false
            }
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/hotm")
        }

        if (shouldExecuteCommand && Minecraft.getMinecraft().currentScreen == null) {
            shouldExecuteCommand = false
            executeCommand()
        }

        if (event?.phase == TickEvent.Phase.END) {
            tickCounter++
            if (tickCounter % 20 == 0) {
                val mc = Minecraft.getMinecraft()
                if (mc.theWorld == null || mc.netHandler == null) return

                if (mc.theWorld.provider.dimensionId != lastWorldId) {
                    notifiedPlayers.clear()
                    lastWorldId = mc.theWorld.provider.dimensionId
                }

                val visiblePlayers = mc.netHandler.playerInfoMap
                    .mapNotNull { it.gameProfile?.name }
                    .filter { it.isNotEmpty() }

                if (visiblePlayers.isEmpty()) return

                val listedNames = config.sigmalist
                    .split(",")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                    .toSet()

                if (listedNames.isEmpty()) return

                val matchedPlayers = visiblePlayers.filter { 
                    val name = it.lowercase()
                    name in listedNames && name !in notifiedPlayers
                }

                if (matchedPlayers.isNotEmpty()) {
                    matchedPlayers.forEach { player ->
                        notifiedPlayers.add(player.lowercase())
                    }
                    mc.ingameGUI.displayTitle(
                        config.title,
                        "§eDetected: §f${matchedPlayers.joinToString(", ")}",
                        config.fadein, config.displaytime, config.fadeout
                    )
                }

                notifiedPlayers.removeAll { player ->
                    !visiblePlayers.any { it.lowercase() == player }
                }
            }
        }
    }

    @SubscribeEvent
    fun onKeyInput(event: InputEvent.KeyInputEvent?) {
        if (KEYBIND_CONFIG.isPressed) {
            try {
                val gui = config.gui()
                if (gui == null) return
                Minecraft.getMinecraft().displayGuiScreen(gui)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    private inner class CommandWaypoints : CommandBase() {
        override fun getCommandName() = "gemstone"
        override fun getCommandUsage(sender: ICommandSender) = "/gemstone waypoints [reset]"
        override fun canCommandSenderUseCommand(sender: ICommandSender): Boolean {
            return true
        }

        override fun processCommand(sender: ICommandSender, args: Array<String>) {
            when (args.getOrNull(0)?.lowercase()) {
                "waypoints" -> {
                    when (args.getOrNull(1)?.lowercase()) {
                        "reset" -> {
                            currentWaypoints = null
                            currentWaypointIndex = -1
                            lastLoadedGemstone = ""
                        }
                        else -> addClientMessage("${EnumChatFormatting.YELLOW}Usage: /gemstone waypoints reset")
                    }
                }
                else -> addClientMessage("${EnumChatFormatting.YELLOW}Usage: /gemstone waypoints [reset]")
            }
        }
    }

    fun getClipboardText(): String? = try {
        Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
    } catch (e: Exception) { null }

    data class Waypoint(val x: Double, val y: Double, val z: Double, val r: Float, val g: Float, val b: Float, val options: Options)
    data class Options(val name: Int)

    private val gson = Gson()

    fun loadWaypointsFromFile(file: File): List<Waypoint>? =
        if (!file.exists()) null else gson.fromJson(file.readText(), object : TypeToken<List<Waypoint>>() {}.type)

    fun parseWaypoints(json: String): List<Waypoint> = gson.fromJson(json, object : TypeToken<List<Waypoint>>() {}.type)

    fun distance(a: Vec3, b: Vec3): Double =
        Math.sqrt((a.xCoord - b.xCoord).let { dx -> dx * dx } + (a.yCoord - b.yCoord).let { dy -> dy * dy } + (a.zCoord - b.zCoord).let { dz -> dz * dz })

    fun getPlayerPos(): Vec3? = Minecraft.getMinecraft().thePlayer?.let { Vec3(it.posX, it.posY, it.posZ) }

    fun findCurrentAndNextWaypoint(waypoints: List<Waypoint>, playerPos: Vec3, threshold: Double = 2.5): Triple<Waypoint?, Waypoint?, Waypoint?> {
        var lastWaypoint: Waypoint? = null
        var currentWaypoint: Waypoint? = null
        var nextWaypoint: Waypoint? = null

        if (currentWaypointIndex == -1 && waypoints.isNotEmpty()) {
            currentWaypointIndex = 0
            currentWaypoint = waypoints[0]
            nextWaypoint = waypoints.getOrNull(1)
            return Triple(lastWaypoint, currentWaypoint, nextWaypoint)
        }

        if (currentWaypointIndex >= waypoints.size) {
            return Triple(null, null, null)
        }

        currentWaypoint = waypoints.getOrNull(currentWaypointIndex)
        nextWaypoint = waypoints.getOrNull(currentWaypointIndex + 1)

        nextWaypoint?.let { next ->
            val nextDist = distance(playerPos, Vec3(next.x, next.y, next.z))
            if (nextDist <= threshold) {
                lastWaypoint = currentWaypoint
                currentWaypoint = next
                currentWaypointIndex++
                nextWaypoint = waypoints.getOrNull(currentWaypointIndex + 1)
            }
        }

        return Triple(lastWaypoint, currentWaypoint, nextWaypoint)
    }

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        if (currentWaypoints == null) return

        val playerPos = getPlayerPos() ?: return
        val (last, current, next) = findCurrentAndNextWaypoint(currentWaypoints!!, playerPos)

        if (config.showWaypointLines) {
            next?.let { nextWaypoint ->
                val nextPos = Vec3(nextWaypoint.x, nextWaypoint.y, nextWaypoint.z)
                drawLine(playerPos, nextPos, event.partialTicks, 1.0f, 1.0f, 0.0f)
            }
        }

        if (config.showWaypoints) {
            current?.let { currentWaypoint ->
                val currentPos = Vec3(currentWaypoint.x, currentWaypoint.y, currentWaypoint.z)
                drawWaypoint(currentPos, 0.0f, 1.0f, 0.0f, event.partialTicks, currentWaypoint)
            }

            last?.let { lastWaypoint ->
                val lastPos = Vec3(lastWaypoint.x, lastWaypoint.y, lastWaypoint.z)
                drawWaypoint(lastPos, 0.0f, 0.0f, 0.5f, event.partialTicks, lastWaypoint)
            }

            next?.let { nextWaypoint ->
                val nextPos = Vec3(nextWaypoint.x, nextWaypoint.y, nextWaypoint.z)
                drawWaypoint(nextPos, 1.0f, 1.0f, 0.0f, event.partialTicks, nextWaypoint)
            }
        }
    }

    fun drawLine(from: Vec3, to: Vec3, partialTicks: Float, r: Float, g: Float, b: Float) {
        val player = Minecraft.getMinecraft().thePlayer ?: return
        val dx = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks
        val dy = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks
        val dz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glLineWidth(2.0f)
        GL11.glBegin(GL11.GL_LINES)
        GL11.glColor3f(r, g, b)

        GL11.glVertex3d(from.xCoord - dx, from.yCoord - dy, from.zCoord - dz)
        GL11.glVertex3d(to.xCoord - dx, to.yCoord - dy, to.zCoord - dz)

        GL11.glEnd()
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    private fun drawWaypoint(pos: Vec3, r: Float, g: Float, b: Float, partialTicks: Float, waypoint: Waypoint) {
        val player = Minecraft.getMinecraft().thePlayer ?: return
        val dx = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks
        val dy = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks
        val dz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glLineWidth(3.0f)

        val x = pos.xCoord - dx
        val y = pos.yCoord - dy
        val z = pos.zCoord - dz

        GL11.glBegin(GL11.GL_QUADS)
        GL11.glColor4f(r, g, b, 0.4f)

        GL11.glVertex3d(x, y, z)
        GL11.glVertex3d(x + 1, y, z)
        GL11.glVertex3d(x + 1, y, z + 1)
        GL11.glVertex3d(x, y, z + 1)

        GL11.glVertex3d(x, y + 1, z)
        GL11.glVertex3d(x + 1, y + 1, z)
        GL11.glVertex3d(x + 1, y + 1, z + 1)
        GL11.glVertex3d(x, y + 1, z + 1)

        GL11.glVertex3d(x, y, z + 1)
        GL11.glVertex3d(x + 1, y, z + 1)
        GL11.glVertex3d(x + 1, y + 1, z + 1)
        GL11.glVertex3d(x, y + 1, z + 1)

        GL11.glVertex3d(x, y, z)
        GL11.glVertex3d(x + 1, y, z)
        GL11.glVertex3d(x + 1, y + 1, z)
        GL11.glVertex3d(x, y + 1, z)

        GL11.glVertex3d(x, y, z)
        GL11.glVertex3d(x, y, z + 1)
        GL11.glVertex3d(x, y + 1, z + 1)
        GL11.glVertex3d(x, y + 1, z)

        GL11.glVertex3d(x + 1, y, z)
        GL11.glVertex3d(x + 1, y, z + 1)
        GL11.glVertex3d(x + 1, y + 1, z + 1)
        GL11.glVertex3d(x + 1, y + 1, z)

        GL11.glEnd()

        GL11.glBegin(GL11.GL_LINES)
        GL11.glColor3f(r, g, b)

        GL11.glVertex3d(x, y, z)
        GL11.glVertex3d(x + 1, y, z)
        GL11.glVertex3d(x + 1, y, z)
        GL11.glVertex3d(x + 1, y, z + 1)
        GL11.glVertex3d(x + 1, y, z + 1)
        GL11.glVertex3d(x, y, z + 1)
        GL11.glVertex3d(x, y, z + 1)
        GL11.glVertex3d(x, y, z)

        GL11.glVertex3d(x, y + 1, z)
        GL11.glVertex3d(x + 1, y + 1, z)
        GL11.glVertex3d(x + 1, y + 1, z)
        GL11.glVertex3d(x + 1, y + 1, z + 1)
        GL11.glVertex3d(x + 1, y + 1, z + 1)
        GL11.glVertex3d(x, y + 1, z + 1)
        GL11.glVertex3d(x, y + 1, z + 1)
        GL11.glVertex3d(x, y + 1, z)

        GL11.glVertex3d(x, y, z)
        GL11.glVertex3d(x, y + 1, z)
        GL11.glVertex3d(x + 1, y, z)
        GL11.glVertex3d(x + 1, y + 1, z)
        GL11.glVertex3d(x + 1, y, z + 1)
        GL11.glVertex3d(x + 1, y + 1, z + 1)
        GL11.glVertex3d(x, y, z + 1)
        GL11.glVertex3d(x, y + 1, z + 1)

        GL11.glEnd()

        val waypointIndex = currentWaypoints?.indexOf(waypoint)?.plus(1) ?: 0
        if (waypointIndex > 0) {
            drawWaypointLabel(x + 0.5, y + 1.2, z + 0.5, waypointIndex.toString())
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    private fun drawWaypointLabel(x: Double, y: Double, z: Double, text: String) {
        val mc = Minecraft.getMinecraft()
        val fontRenderer = mc.fontRendererObj
        val scale = 0.016666668f * 2.0f

        GL11.glPushMatrix()
        GL11.glTranslatef(x.toFloat(), y.toFloat(), z.toFloat())
        GL11.glNormal3f(0.0f, 1.0f, 0.0f)
        GL11.glRotatef(-mc.renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
        GL11.glRotatef(mc.renderManager.playerViewX, 1.0f, 0.0f, 0.0f)
        GL11.glScalef(-scale, -scale, scale)
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDepthMask(false)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glDisable(GL11.GL_TEXTURE_2D)

        val width = fontRenderer.getStringWidth(text)
        GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f)
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex2f(-width / 2f - 4, -4f)
        GL11.glVertex2f(-width / 2f - 4, 12f)
        GL11.glVertex2f(width / 2f + 4, 12f)
        GL11.glVertex2f(width / 2f + 4, -4f)
        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        fontRenderer.drawStringWithShadow(text, -width / 2f, 0f, 0xFFFFFF)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(true)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
    }
}
object WaypointState {
    var currentPair: Pair<HsbUtils.Waypoint?, HsbUtils.Waypoint?>? = null
}