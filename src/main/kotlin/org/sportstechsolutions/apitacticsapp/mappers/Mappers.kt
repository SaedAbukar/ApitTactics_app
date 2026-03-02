package org.sportstechsolutions.apitacticsapp.mappers

import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.TeamRepository

// -------------------------------------------------------------------
// EXTENSION FUNCTIONS (For Request Mapping)
// -------------------------------------------------------------------

fun List<FormationPositionRequest>.toFormationPositions(
    user: User,
    teamRepository: TeamRepository
): MutableList<FormationPosition> {
    return this.map { req ->
        val team = if (!req.teamName.isNullOrBlank()) {
            teamRepository.findByOwnerIdAndName(user.id, req.teamName)
                ?: teamRepository.save(
                    Team(
                        name = req.teamName,
                        color = req.teamColor ?: "white",
                        owner = user
                    )
                )
        } else {
            null
        }

        FormationPosition(
            x = req.x,
            y = req.y,
            team = team
        )
    }.toMutableList()
}

// -------------------------------------------------------------------
// RESPONSE MAPPERS (Static Objects)
// -------------------------------------------------------------------

object PracticeMapper {
    fun toPracticeResponse(practice: Practice, currentUserId: Int): PracticeResponse {
        return PracticeResponse(
            id = practice.id ?: 0,
            name = practice.name ?: "",
            description = practice.description ?: "",
            isPremade = practice.isPremade,
            isPublic = practice.isPublic, // MAP VISIBILITY
            ownerId = practice.owner?.id ?: 0,
            role = AccessRole.NONE,

            // Re-using the SessionMapper logic to ensure child sessions are fully mapped
            sessions = practice.sessions.map { SessionMapper.toSessionResponse(it, currentUserId) },

            // Taxonomy & Filters
            phaseOfPlay = practice.phaseOfPlay,
            ballContext = practice.ballContext,
            drillFormat = practice.drillFormat,
            minPlayers = practice.minPlayers,
            maxPlayers = practice.maxPlayers,
            durationMinutes = practice.durationMinutes,
            areaSize = practice.areaSize,
            targetAgeLevel = practice.targetAgeLevel,
            tacticalActions = practice.tacticalActions.toSet(),
            qualityMakers = practice.qualityMakers.toSet(),

            // Engagement
            viewCount = practice.viewCount,
            isFavorite = practice.favoritedByUsers.any { it.id == currentUserId }
        )
    }

    // Shared Step Mapper
    fun toStepResponse(step: Step): StepResponse {
        return StepResponse(
            id = step.id,
            players = step.players.map { p -> PlayerResponse(p.id ?: 0, p.x, p.y, p.number, p.color ?: "", p.team?.id) },
            balls = step.balls.map { b -> BallResponse(b.id ?: 0, b.x, b.y, b.color) },
            goals = step.goals.map { g -> GoalResponse(g.id ?: 0, g.x, g.y, g.width, g.depth, g.color, g.rotation) },
            teams = step.teams.map { t -> TeamResponse(t.id ?: 0, t.name ?: "", t.color ?: "") },
            formations = step.formations.map { f ->
                FormationResponse(
                    id = f.id,
                    name = f.name,
                    positions = f.positions.map { p -> FormationPositionResponse(p.id ?: 0, p.x, p.y, p.team?.id) }
                )
            },
            cones = step.cones.map { c -> ConeResponse(c.id ?: 0, c.x, c.y, c.color) }
        )
    }
}

object GameTacticMapper {
    fun toGameTacticResponse(gameTactic: GameTactic, currentUserId: Int): GameTacticResponse {
        return GameTacticResponse(
            id = gameTactic.id ?: 0,
            name = gameTactic.name ?: "",
            description = gameTactic.description ?: "",
            isPremade = gameTactic.isPremade,
            isPublic = gameTactic.isPublic, // MAP VISIBILITY
            ownerId = gameTactic.owner?.id ?: 0,
            role = AccessRole.NONE,

            // Re-using the SessionMapper logic
            sessions = gameTactic.sessions.map { SessionMapper.toSessionResponse(it, currentUserId) },

            // Engagement
            viewCount = gameTactic.viewCount,
            isFavorite = gameTactic.favoritedByUsers.any { it.id == currentUserId }
        )
    }
}

object SessionMapper {
    // This is the base mapper used by EntityMappers.loadFullSession
    fun toSessionResponse(session: Session, currentUserId: Int): SessionResponse {
        return SessionResponse(
            id = session.id ?: 0,
            name = session.name ?: "",
            description = session.description ?: "",
            isPremade = session.isPremade,
            isPublic = session.isPublic, // MAP VISIBILITY
            ownerId = session.owner?.id ?: 0,
            role = AccessRole.NONE,
            steps = session.steps.map { PracticeMapper.toStepResponse(it) },

            // AVOID CIRCULAR REFERENCE: Map parent IDs only
            practiceIds = session.practices.mapNotNull { it.id },
            gameTacticIds = session.gameTactics.mapNotNull { it.id },

            // Taxonomy & Filters
            phaseOfPlay = session.phaseOfPlay,
            ballContext = session.ballContext,
            drillFormat = session.drillFormat,
            minPlayers = session.minPlayers,
            maxPlayers = session.maxPlayers,
            durationMinutes = session.durationMinutes,
            areaSize = session.areaSize,
            targetAgeLevel = session.targetAgeLevel,
            tacticalActions = session.tacticalActions.toSet(),
            qualityMakers = session.qualityMakers.toSet(),

            // Engagement
            viewCount = session.viewCount,
            isFavorite = session.favoritedByUsers.any { it.id == currentUserId }
        )
    }
}