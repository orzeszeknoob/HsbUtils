package com.orzeszek.config

import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
import java.io.File

class HsbUtilsConfig : Vigilant(File("config/hsbutils/config.toml"), "Hsb Utils") {
    @Property(
        type = PropertyType.SWITCH,
        name = "Enable HOTM Macro",
        description = "Enable automatic HOTM command execution when entering/leaving mineshafts (Use at your own risk)",
        category = "Mineshaft Mining"
    )
    var enableHotmMacro = false

    @Property(
        type = PropertyType.SLIDER,
        name = "Mineshaft Delay",
        description = "Delay in milliseconds before executing mineshaft commands",
        category = "Mineshaft Mining",
        min = 500,
        max = 1000
    )
    var mineshaftDelay = 500

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Waypoints",
        description = "Show waypoints in the world",
        category = "Waypoints"
    )
    var showWaypoints = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Waypoint Labels",
        description = "Show labels on waypoints",
        category = "Waypoints"
    )
    var showWaypointLabels = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Waypoint Lines",
        description = "Show lines connecting waypoints",
        category = "Waypoints"
    )
    var showWaypointLines = true

    @Property(
        type = PropertyType.TEXT,
        name = "Player Watch List",
        description = "Comma-separated list of players to watch for (case insensitive)",
        category = "Player Detection"
    )
    var sigmalist = ""

    @Property(
        type = PropertyType.TEXT,
        name = "Detection Title",
        description = "Title to display when a watched player is detected",
        category = "Player Detection"
    )
    var title = "Player Detected!"
    @Property(
        type = PropertyType.SLIDER,
        name = "Fade in time",
        description = "fade in time of the title",
        category = "Player Detection",
        min = 100,
        max = 200
    )
    var fadein = 100
    @Property(
        type = PropertyType.SLIDER,
        name = "Display time",
        description = "display time of the title",
        category = "Player Detection",
        min = 250,
        max = 1000
    )
    var displaytime = 500
    @Property(
        type = PropertyType.SLIDER,
        name = "Fade out time",
        description = "fade out time for the title",
        category = "Player Detection",
        min = 100,
        max = 200
    )
    var fadeout = 100

    init {
        initialize()
        addDependency("showWaypointLabels", "showWaypoints")
        addDependency("showWaypointLines", "showWaypoints")
        addDependency("mineshaftDelay", "enableHotmMacro")
    }
} 