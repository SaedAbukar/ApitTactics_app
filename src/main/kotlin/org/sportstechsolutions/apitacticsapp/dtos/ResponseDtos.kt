package org.sportstechsolutions.apitacticsapp.dtos

import org.sportstechsolutions.apitacticsapp.model.AccessRole

data class PracticeResponse(
    val id: Int,
    val name: String,
    val description: String,
    val isPremade: Boolean,
    val sessions: List<SessionResponse>
)

data class GameTacticResponse(
    val id: Int,
    val name: String,
    val description: String,
    val isPremade: Boolean,
    val sessions: List<SessionResponse>
)

data class SessionResponse(
    val id: Int,
    val name: String,
    val description: String,
    val steps: List<StepResponse>
)

data class TabbedResponse<T>(
    val personalItems: List<T> = emptyList(),
    val userSharedItems: List<T> = emptyList(),
    val groupSharedItems: List<T> = emptyList()
)

data class ShareResponse(
    val sessionId: Int,
    val targetId: Int,
    val role: AccessRole,
    val message: String
)

data class StepResponse(
    val id: Int,
    val players: List<PlayerResponse>,
    val balls: List<BallResponse>,
    val goals: List<GoalResponse>,
    val teams: List<TeamResponse>,
    val formations: List<FormationResponse>,
    val cones: List<ConeResponse>
)

data class PlayerResponse(val id: Int, val x: Int, val y: Int, val number: Int, val color: String, val teamId: Int?)
data class BallResponse(val id: Int, val x: Int, val y: Int, val color: String?)
data class GoalResponse(val id: Int, val x: Int, val y: Int, val width: Int, val depth: Int, val color: String?)
data class TeamResponse(val id: Int, val name: String, val color: String)
data class FormationResponse(val id: Int, val name: String, val positions: List<FormationPositionResponse>)
data class FormationPositionResponse(val id: Int, val x: Double, val y: Double, val teamId: Int?)
data class ConeResponse(val id: Int, val x: Int, val y: Int, val color: String?)
