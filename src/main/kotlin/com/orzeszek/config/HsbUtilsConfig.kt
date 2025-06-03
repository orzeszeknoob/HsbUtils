package com.orzeszek.config

import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
import java.io.File

class HsbUtilsConfig : Vigilant(File("config/hsbutils/config.toml"), "Gemstone Mod") {
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

    init {
        initialize()
        addDependency("showWaypointLabels", "showWaypoints")
        addDependency("showWaypointLines", "showWaypoints")
        addDependency("mineshaftDelay", "enableHotmMacro")
    }
} 