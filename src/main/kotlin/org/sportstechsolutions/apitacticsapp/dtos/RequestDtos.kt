package org.sportstechsolutions.apitacticsapp.dtos

data class PracticeRequest(
    val name: String,
    val description: String,
    val isPremade: Boolean = false,
    val sessions: List<SessionRequest> = emptyList()
)

data class GameTacticRequest(
    val name: String,
    val description: String,
    val isPremade: Boolean = false,
    val sessions: List<SessionRequest> = emptyList()
)

data class SessionRequest(
    val name: String,
    val description: String,
    val steps: List<StepRequest> = emptyList()
)

data class StepRequest(
    val players: List<PlayerRequest> = emptyList(),
    val balls: List<BallRequest> = emptyList(),
    val goals: List<GoalRequest> = emptyList(),
    val teams: List<TeamRequest> = emptyList(),
    val formations: List<FormationRequest> = emptyList(),
    val cones: List<ConeRequest> = emptyList()
)

data class PlayerRequest(val x: Int, val y: Int, val number: Int, val color: String, val teamName: String?)
data class BallRequest(val x: Int, val y: Int, val color: String?)
data class GoalRequest(val x: Int, val y: Int, val width: Int, val depth: Int, val color: String?)
data class TeamRequest(val name: String, val color: String)
data class FormationRequest(val id: Int?, val name: String, val positions: List<FormationPositionRequest> = emptyList())
data class FormationPositionRequest(val x: Double, val y: Double, val teamName: String?, val teamColor: String?)

data class ConeRequest(val x: Int, val y: Int, val color: String?)
