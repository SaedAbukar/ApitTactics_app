package org.sportstechsolutions.apitacticsapp.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import org.sportstechsolutions.apitacticsapp.model.AccessRole

data class PublicUserResponse(
    val id: Int,
    val email: String,
    @get:JsonProperty("isPublic")
    val isPublic: Boolean
)

data class UserProfileResponse(
    val id: Int,
    val email: String,
    @get:JsonProperty("isPublic")
    val isPublic: Boolean,
    val message: String? = null
)

// --- 1. LIGHTWEIGHT SUMMARY (For Tabs) ---
data class PracticeSummaryResponse(
    val id: Int,
    val name: String,
    val description: String,
    val isPremade: Boolean,
    val ownerId: Int,
    val sessions: List<SessionSummaryResponse>, // <--- Changed from sessionCount
    val role: AccessRole
)

// --- 2. FULL DETAIL RESPONSE (For Tactic Board) ---
data class PracticeResponse(
    val id: Int,
    val name: String,
    val description: String,
    val isPremade: Boolean,
    val ownerId: Int,
    val role: AccessRole,
    val sessions: List<SessionResponse>
)

// --- 1. LIGHTWEIGHT SUMMARY (For Tabs) ---
data class GameTacticSummaryResponse(
    val id: Int,
    val name: String,
    val description: String,
    val isPremade: Boolean,
    val ownerId: Int,
    val sessions: List<SessionSummaryResponse>, // <--- Changed from sessionCount
    val role: AccessRole
)

// --- 2. FULL DETAIL RESPONSE (For Tactic Board) ---
data class GameTacticResponse(
    val id: Int,
    val name: String,
    val description: String,
    val isPremade: Boolean,
    val ownerId: Int,
    val role: AccessRole,
    val sessions: List<SessionResponse>
)

// 1. The Lightweight Summary (For Tabs)
data class SessionSummaryResponse(
    val id: Int,
    val name: String,
    val description: String,
    val ownerId: Int,
    val stepCount: Int,
    val role: AccessRole // <--- Added
)

// 2. The Full Detail Response (For the Tactic Board)
// Ensure your existing SessionResponse looks like this or has these fields added
data class SessionResponse(
    val id: Int,
    val name: String,
    val description: String,
    val ownerId: Int,
    val steps: List<StepResponse>, // The heavy list
    val role: AccessRole // <--- Added
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
