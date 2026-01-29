package org.sportstechsolutions.apitacticsapp.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.sportstechsolutions.apitacticsapp.model.AccessRole
import jakarta.validation.constraints.NotNull

data class UpdateVisibilityRequest(
    @field:NotNull(message = "Visibility status is required")
    @param:JsonProperty("isPublic") // Required for constructor/input
    @get:JsonProperty("isPublic")   // Required for output (if ever returned)
    val isPublic: Boolean = false
)

data class ShareSessionRequest(
    @field:NotNull val sessionId: Int,
    @field:NotNull val targetId: Int, // either userId or groupId depending on context
    val role: AccessRole = AccessRole.VIEWER
)

data class RevokeSessionRequest(
    @field:NotNull val sessionId: Int,
    @field:NotNull val targetId: Int
)

data class SharePracticeRequest(
    @field:NotNull val practiceId: Int,
    @field:NotNull val targetId: Int, // either userId or groupId depending on context
    val role: AccessRole = AccessRole.VIEWER
)

data class RevokePracticeRequest(
    @field:NotNull val practiceId: Int,
    @field:NotNull val targetId: Int
)

data class ShareGameTacticRequest(
    @field:NotNull val gameTacticId: Int,
    @field:NotNull val targetId: Int, // either userId or groupId depending on context
    val role: AccessRole = AccessRole.VIEWER
)

data class RevokeGameTacticRequest(
    @field:NotNull val gameTacticId: Int,
    @field:NotNull val targetId: Int
)


data class PracticeRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,

    @field:NotBlank(message = "Description is required")
    val description: String,

    val isPremade: Boolean = false,

    @field:Valid
    val sessions: List<@Valid SessionRequest> = emptyList()
)

data class GameTacticRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,

    @field:NotBlank(message = "Description is required")
    val description: String,

    val isPremade: Boolean = false,

    @field:Valid
    val sessions: List<@Valid SessionRequest> = emptyList()
)

// -----------------------------------------------------------
// HYBRID REQUEST: Supports Creation (Data required) & Linking (ID only)
// -----------------------------------------------------------
data class SessionRequest(
    val id: Int? = null,

    // Nullable to allow linking existing sessions by ID only.
    // Validation is now handled in EntityMappers.toSession()
    val name: String? = null,
    val description: String? = null,

    @field:Valid
    val steps: List<@Valid StepRequest> = emptyList()
)

data class StepRequest(
    @field:Valid
    val players: List<@Valid PlayerRequest> = emptyList(),

    @field:Valid
    val balls: List<@Valid BallRequest> = emptyList(),

    @field:Valid
    val goals: List<@Valid GoalRequest> = emptyList(),

    @field:Valid
    val teams: List<@Valid TeamRequest> = emptyList(),

    @field:Valid
    val formations: List<@Valid FormationRequest> = emptyList(),

    @field:Valid
    val cones: List<@Valid ConeRequest> = emptyList()
)

data class PlayerRequest(
    @field:Min(value = 0, message = "X coordinate must be non-negative")
    val x: Int,

    @field:Min(value = 0, message = "Y coordinate must be non-negative")
    val y: Int,

    @field:Min(value = 1, message = "Player number must be at least 1")
    val number: Int,

    @field:NotBlank(message = "Color is required")
    val color: String,

    val teamName: String? = null
)

data class BallRequest(
    @field:Min(value = 0, message = "X coordinate must be non-negative")
    val x: Int,

    @field:Min(value = 0, message = "Y coordinate must be non-negative")
    val y: Int,

    val color: String? = null
)

data class GoalRequest(
    @field:Min(value = 0, message = "X coordinate must be non-negative")
    val x: Int,

    @field:Min(value = 0, message = "Y coordinate must be non-negative")
    val y: Int,

    @field:Min(value = 1, message = "Width must be at least 1")
    val width: Int,

    @field:Min(value = 1, message = "Depth must be at least 1")
    val depth: Int,

    val color: String? = null
)

data class TeamRequest(
    @field:NotBlank(message = "Team name is required")
    val name: String,

    @field:NotBlank(message = "Team color is required")
    val color: String
)

data class FormationRequest(
    val id: Int? = null,

    @field:NotBlank(message = "Formation name is required")
    val name: String,

    @field:Valid
    val positions: List<@Valid FormationPositionRequest> = emptyList()
)

data class FormationPositionRequest(
    @field:Min(value = 0, message = "X coordinate must be non-negative")
    val x: Double,

    @field:Min(value = 0, message = "Y coordinate must be non-negative")
    val y: Double,

    val teamName: String? = null,
    val teamColor: String? = null
)

data class ConeRequest(
    @field:Min(value = 0, message = "X coordinate must be non-negative")
    val x: Int,

    @field:Min(value = 0, message = "Y coordinate must be non-negative")
    val y: Int,

    val color: String? = null
)
