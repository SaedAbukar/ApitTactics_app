package org.sportstechsolutions.apitacticsapp.dtos
import org.sportstechsolutions.apitacticsapp.mappers.*
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.TeamRepository
import org.springframework.stereotype.Component

@Component
class EntityMappers(private val teamRepository: TeamRepository) {

    // ---------------- SESSION / PRACTICE ----------------
    fun toSession(req: SessionRequest, owner: User): Session {
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

    // ---------------- STEP ----------------
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

    // ---------------- FULL FETCH ----------------
    fun loadFullSession(session: Session): SessionResponse {
        session.steps.forEach { step ->
            step.players.size
            step.balls.size
            step.goals.size
            step.teams.size
            step.formations.forEach { it.positions.size }
            step.cones.size
        }
        return SessionMapper.toSessionResponse(session)
    }

    fun loadFullPractice(practice: Practice): PracticeResponse {
        practice.sessions.forEach { session ->
            session.steps.forEach { step ->
                step.players.size
                step.balls.size
                step.goals.size
                step.teams.size
                step.formations.forEach { it.positions.size }
                step.cones.size
            }
        }
        return PracticeMapper.toPracticeResponse(practice)
    }

    fun loadFullGameTactic(gameTactic: GameTactic): GameTacticResponse {
        gameTactic.sessions.forEach { session ->
            session.steps.forEach { step ->
                step.players.size
                step.balls.size
                step.goals.size
                step.teams.size
                step.formations.forEach { it.positions.size }
                step.cones.size
            }
        }
        return GameTacticMapper.toGameTacticResponse(gameTactic)
    }
}

