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
        val team = req.teamName?.let { name ->
            teamRepository.findByOwnerIdAndName(user.id, name)
                ?: teamRepository.save(
                    Team(
                        name = name,
                        color = req.teamColor ?: "white", // use request color, fallback to white
                        owner = user
                    )
                )
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
    fun toPracticeResponse(practice: Practice): PracticeResponse {
        return PracticeResponse(
            id = practice.id,
            name = practice.name,
            description = practice.description,
            isPremade = practice.is_premade,
            ownerId = practice.owner?.id ?: 0,
            role = AccessRole.NONE, // Default, overwritten by EntityMappers
            sessions = practice.sessions.map { toSessionResponse(it) }
        )
    }

    // Helper for nested sessions within a Practice
    internal fun toSessionResponse(session: Session): SessionResponse {
        return SessionResponse(
            id = session.id,
            name = session.name,
            description = session.description,
            ownerId = session.owner?.id ?: 0,
            // Inside a Practice, we default the session-specific role (usually Viewer or None)
            // The UI context for a Practice is usually different from a standalone Session
            role = AccessRole.NONE,
            steps = session.steps.map { toStepResponse(it) }
        )
    }

    // Shared Step Mapper (Used by Practice, GameTactic, and Session Mappers)
    fun toStepResponse(step: Step): StepResponse {
        return StepResponse(
            id = step.id,
            players = step.players.map { p -> PlayerResponse(p.id, p.x, p.y, p.number, p.color, p.team?.id) },
            balls = step.balls.map { b -> BallResponse(b.id, b.x, b.y, b.color) },
            goals = step.goals.map { g -> GoalResponse(g.id, g.x, g.y, g.width, g.depth, g.color) },
            teams = step.teams.map { t -> TeamResponse(t.id, t.name, t.color) },
            formations = step.formations.map { f ->
                FormationResponse(
                    id = f.id,
                    name = f.name,
                    positions = f.positions.map { p -> FormationPositionResponse(p.id, p.x, p.y, p.team?.id) }
                )
            },
            cones = step.cones.map { c -> ConeResponse(c.id, c.x, c.y, c.color) }
        )
    }
}

object GameTacticMapper {
    fun toGameTacticResponse(gameTactic: GameTactic): GameTacticResponse {
        return GameTacticResponse(
            id = gameTactic.id,
            name = gameTactic.name,
            description = gameTactic.description,
            isPremade = gameTactic.is_premade,
            ownerId = gameTactic.owner?.id ?: 0,
            role = AccessRole.NONE, // Default, overwritten by EntityMappers
            // Reuse the PracticeMapper session logic for consistency
            sessions = gameTactic.sessions.map { PracticeMapper.toSessionResponse(it) }
        )
    }
}

object SessionMapper {
    // This is the base mapper used by EntityMappers.loadFullSession
    fun toSessionResponse(session: Session): SessionResponse {
        return SessionResponse(
            id = session.id,
            name = session.name,
            description = session.description,
            ownerId = session.owner?.id ?: 0,
            // Default role is NONE here.
            // It is vital that EntityMappers.loadFullSession overwrites this
            // using .copy(role = calculatedRole)
            role = AccessRole.NONE,
            steps = session.steps.map { PracticeMapper.toStepResponse(it) }
        )
    }
}