package org.sportstechsolutions.apitacticsapp.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import org.sportstechsolutions.apitacticsapp.model.*

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

// --- PAGINATION WRAPPERS ---
data class PagedResponse<T>(
    val content: List<T>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
)

data class TabbedResponse<T>(
    val personalItems: PagedResponse<T>,
    val userSharedItems: PagedResponse<T>,
    val groupSharedItems: PagedResponse<T>
)

data class ShareResponse(
    val sessionId: Int,
    val targetId: Int,
    val role: AccessRole,
    val message: String
)

// --- 1. LIGHTWEIGHT SUMMARIES (For Tabs & Search) ---

data class PracticeSummaryResponse(
    val id: Int,
    val name: String,
    val description: String,
    val isPremade: Boolean,
    @get:JsonProperty("isPublic")
    val isPublic: Boolean, // Added for visibility
    val ownerId: Int,
    val sessions: List<SessionSummaryResponse>,
    val role: AccessRole,

    // Taxonomy & Filters
    val phaseOfPlay: PhaseOfPlay?,
    val ballContext: BallContext?,
    val drillFormat: DrillFormat?,
    val minPlayers: Int?,
    val maxPlayers: Int?,
    val durationMinutes: Int?,
    val areaSize: String?,
    val targetAgeLevel: String?,
    val tacticalActions: Set<TacticalAction>,
    val qualityMakers: Set<QualityMaker>,

    // Engagement
    val viewCount: Int,
    val isFavorite: Boolean
)

data class SessionSummaryResponse(
    val id: Int,
    val name: String,
    val description: String,
    val isPremade: Boolean,
    @get:JsonProperty("isPublic")
    val isPublic: Boolean, // Added for visibility
    val ownerId: Int,
    val stepCount: Int,
    val role: AccessRole,

    // Parent Tracking (Anti-Circular)
    val practiceIds: List<Int>,
    val gameTacticIds: List<Int>,

    // Taxonomy & Filters
    val phaseOfPlay: PhaseOfPlay?,
    val ballContext: BallContext?,
    val drillFormat: DrillFormat?,
    val minPlayers: Int?,
    val maxPlayers: Int?,
    val durationMinutes: Int?,
    val areaSize: String?,
    val targetAgeLevel: String?,
    val tacticalActions: Set<TacticalAction>,
    val qualityMakers: Set<QualityMaker>,

    // Engagement
    val viewCount: Int,
    val isFavorite: Boolean
)

data class GameTacticSummaryResponse(
    val id: Int,
    val name: String,
    val description: String,
    val isPremade: Boolean,
    @get:JsonProperty("isPublic")
    val isPublic: Boolean, // Added for visibility
    val ownerId: Int,
    val sessions: List<SessionSummaryResponse>,
    val role: AccessRole,

    // Engagement
    val viewCount: Int,
    val isFavorite: Boolean
)


// --- 2. FULL DETAIL RESPONSES (For Tactic Board & Execution) ---

data class PracticeResponse(
    val id: Int,
    val name: String,
    val description: String,
    val isPremade: Boolean,
    @get:JsonProperty("isPublic")
    val isPublic: Boolean,
    val ownerId: Int,
    val role: AccessRole,
    val sessions: List<SessionResponse>,

    // Taxonomy & Filters
    val phaseOfPlay: PhaseOfPlay?,
    val ballContext: BallContext?,
    val drillFormat: DrillFormat?,
    val minPlayers: Int?,
    val maxPlayers: Int?,
    val durationMinutes: Int?,
    val areaSize: String?,
    val targetAgeLevel: String?,
    val tacticalActions: Set<TacticalAction>,
    val qualityMakers: Set<QualityMaker>,

    // Engagement
    val viewCount: Int,
    val isFavorite: Boolean
)

data class SessionResponse(
    val id: Int,
    val name: String,
    val description: String,
    val isPremade: Boolean,
    @get:JsonProperty("isPublic")
    val isPublic: Boolean,
    val ownerId: Int,
    val steps: List<StepResponse>, // The heavy list
    val role: AccessRole,

    // Parent Tracking (Anti-Circular)
    val practiceIds: List<Int>,
    val gameTacticIds: List<Int>,

    // Taxonomy & Filters
    val phaseOfPlay: PhaseOfPlay?,
    val ballContext: BallContext?,
    val drillFormat: DrillFormat?,
    val minPlayers: Int?,
    val maxPlayers: Int?,
    val durationMinutes: Int?,
    val areaSize: String?,
    val targetAgeLevel: String?,
    val tacticalActions: Set<TacticalAction>,
    val qualityMakers: Set<QualityMaker>,

    // Engagement
    val viewCount: Int,
    val isFavorite: Boolean
)

data class GameTacticResponse(
    val id: Int,
    val name: String,
    val description: String,
    val isPremade: Boolean,
    @get:JsonProperty("isPublic")
    val isPublic: Boolean,
    val ownerId: Int,
    val role: AccessRole,
    val sessions: List<SessionResponse>,

    // Engagement
    val viewCount: Int,
    val isFavorite: Boolean
)

// --- 3. CANVAS ELEMENTS (Unchanged) ---

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
data class GoalResponse(val id: Int, val x: Int, val y: Int, val width: Int, val depth: Int, val color: String?, val rotation: Int)
data class TeamResponse(val id: Int, val name: String, val color: String)
data class FormationResponse(val id: Int, val name: String, val positions: List<FormationPositionResponse>)
data class FormationPositionResponse(val id: Int, val x: Double, val y: Double, val teamId: Int?)
data class ConeResponse(val id: Int, val x: Int, val y: Int, val color: String?)