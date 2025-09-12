package org.sportstechsolutions.apitacticsapp.mappers

import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.model.*

fun List<FormationPositionRequest>.toFormationPositions(): MutableList<FormationPosition> =
    map { FormationPosition(x = it.x, y = it.y, team = it.teamId?.let { Team(id = it) }) }.toMutableList()

object PracticeMapper {
    fun toPracticeResponse(practice: Practice): PracticeResponse {
        return PracticeResponse(
            id = practice.id,
            name = practice.name,
            description = practice.description,
            isPremade = practice.is_premade,
            sessions = practice.sessions.map { toSessionResponse(it) }
        )
    }

    private fun toSessionResponse(session: Session): SessionResponse {
        return SessionResponse(
            id = session.id,
            name = session.name,
            description = session.description,
            steps = session.steps.map { toStepResponse(it) }
        )
    }

    private fun toStepResponse(step: Step): StepResponse {
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
