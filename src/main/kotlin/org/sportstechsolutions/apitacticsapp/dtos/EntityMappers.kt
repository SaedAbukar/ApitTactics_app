package org.sportstechsolutions.apitacticsapp.dtos

import org.sportstechsolutions.apitacticsapp.mappers.*
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.TeamRepository
import org.springframework.stereotype.Component

@Component
class EntityMappers(private val teamRepository: TeamRepository) {

    // ------------------------------------------------------------
    // SESSION CREATION (With Manual Validation)
    // ------------------------------------------------------------
    fun toSession(req: SessionRequest, owner: User): Session {
        // Validation: Since DTO allows nulls (for linking), we must check here for creation.
        if (req.name.isNullOrBlank()) {
            throw IllegalArgumentException("Session name is required when creating a new session")
        }
        if (req.description.isNullOrBlank()) {
            throw IllegalArgumentException("Session description is required when creating a new session")
        }

        val session = Session(
            name = req.name,
            description = req.description,
            owner = owner
        )
        session.steps.addAll(req.steps.map { toStep(it, session, owner) })
        return session
    }

    fun toSession(req: SessionRequest, practice: Practice): Session {
        val owner = practice.owner ?: throw IllegalStateException("Practice owner must not be null")
        return toSession(req, owner)
    }

    fun toSession(req: SessionRequest, gameTactic: GameTactic): Session {
        val owner = gameTactic.owner ?: throw IllegalStateException("GameTactic owner must not be null")
        return toSession(req, owner)
    }

    // ------------------------------------------------------------
    // STEP CREATION
    // ------------------------------------------------------------
    fun toStep(req: StepRequest, session: Session, user: User): Step {
        val step = Step(session = session)

        // Map players
        step.players.addAll(req.players.map { p ->
            val team = p.teamName?.let { name ->
                teamRepository.findByOwnerIdAndName(user.id, name)
                    ?: teamRepository.save(Team(name = name, color = p.color, owner = user))
            }
            Player(step = step, x = p.x, y = p.y, number = p.number, color = p.color, team = team)
        })

        // Map balls, goals, cones
        step.balls.addAll(req.balls.map { Ball(step = step, x = it.x, y = it.y, color = it.color) })
        step.goals.addAll(req.goals.map { Goal(step = step, x = it.x, y = it.y, width = it.width, depth = it.depth, color = it.color) })
        step.cones.addAll(req.cones.map { Cone(step = step, x = it.x, y = it.y, color = it.color) })

        // Map teams
        req.teams.forEach { t ->
            val team = teamRepository.findByOwnerIdAndName(user.id, t.name)
                ?: teamRepository.save(Team(name = t.name, color = t.color, owner = user))
            step.teams.add(team)
        }

        // Map formations
        step.formations.addAll(req.formations.map { f ->
            Formation(step = step, name = f.name, positions = f.positions.toFormationPositions(user, teamRepository))
        })

        return step
    }

    // ------------------------------------------------------------
    // RESPONSE MAPPING (Summaries & Full)
    // ------------------------------------------------------------

    // SESSION
    fun toSessionSummary(session: Session, role: AccessRole): SessionSummaryResponse {
        return SessionSummaryResponse(
            id = session.id,
            name = session.name,
            description = session.description,
            ownerId = session.owner?.id ?: 0,
            stepCount = session.steps.size,
            role = role
        )
    }

    fun loadFullSession(session: Session, role: AccessRole = AccessRole.OWNER): SessionResponse {
        // Trigger lazy loading for batch fetching
        session.steps.forEach { step ->
            step.players.size; step.balls.size; step.goals.size; step.teams.size; step.cones.size
            step.formations.forEach { it.positions.size }
        }
        val baseResponse = SessionMapper.toSessionResponse(session)
        return baseResponse.copy(role = role)
    }

    // PRACTICE
    fun toPracticeSummary(practice: Practice, role: AccessRole): PracticeSummaryResponse {
        return PracticeSummaryResponse(
            id = practice.id,
            name = practice.name,
            description = practice.description,
            isPremade = practice.is_premade,
            ownerId = practice.owner?.id ?: 0,
            sessions = practice.sessions.map { session ->
                toSessionSummary(session, AccessRole.NONE)
            },
            role = role
        )
    }

    fun loadFullPractice(practice: Practice, role: AccessRole = AccessRole.OWNER): PracticeResponse {
        practice.sessions.forEach { session ->
            session.steps.forEach { step ->
                step.players.size; step.balls.size; step.goals.size; step.teams.size; step.cones.size
                step.formations.forEach { it.positions.size }
            }
        }
        val baseResponse = PracticeMapper.toPracticeResponse(practice)
        return baseResponse.copy(role = role, ownerId = practice.owner?.id ?: 0)
    }

    // GAME TACTIC
    fun toGameTacticSummary(gameTactic: GameTactic, role: AccessRole): GameTacticSummaryResponse {
        return GameTacticSummaryResponse(
            id = gameTactic.id,
            name = gameTactic.name,
            description = gameTactic.description,
            isPremade = gameTactic.is_premade,
            ownerId = gameTactic.owner?.id ?: 0,
            sessions = gameTactic.sessions.map { session ->
                toSessionSummary(session, AccessRole.NONE)
            },
            role = role
        )
    }

    fun loadFullGameTactic(gameTactic: GameTactic, role: AccessRole = AccessRole.OWNER): GameTacticResponse {
        gameTactic.sessions.forEach { session ->
            session.steps.forEach { step ->
                step.players.size; step.balls.size; step.goals.size; step.teams.size; step.cones.size
                step.formations.forEach { it.positions.size }
            }
        }
        val baseResponse = GameTacticMapper.toGameTacticResponse(gameTactic)
        return baseResponse.copy(role = role, ownerId = gameTactic.owner?.id ?: 0)
    }
}