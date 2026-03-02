package org.sportstechsolutions.apitacticsapp.model

enum class DrillFormat(val category: String) {
    CIRCUIT("Closed"),
    CLOSED_DRILL("Closed"),
    OPEN_DRILL("Closed"),
    MULTI_SIDED_GAME("Small-Sided Games"),
    DIRECTIONLESS_POSSESSION("Small-Sided Games"),
    WAVE_GAME("Small-Sided Games"),
    PROGRESSION_GAME("Small-Sided Games"),
    GAME_TO_TWO_GOALS("Small-Sided Games"),
    MATCH_PLAY("Open Games")
}