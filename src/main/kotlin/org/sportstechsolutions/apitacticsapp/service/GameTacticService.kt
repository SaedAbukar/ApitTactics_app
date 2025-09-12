package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.mappers.GameTacticMapper
import org.sportstechsolutions.apitacticsapp.mappers.toFormationPositions
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.GameTacticRepository
import org.sportstechsolutions.apitacticsapp.repository.TeamRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GameTacticService(
    private val gameTacticRepository: GameTacticRepository,
    private val teamRepository: TeamRepository
) {

    // ---------------- CREATE ----------------
    @Transactional
    fun createGameTactic(userId: Int, request: GameTacticRequest): GameTacticResponse {
        val gameTactic = GameTactic(
            name = request.name,
            description = request.description,
            is_premade = request.isPremade,
            owner = User(id = userId)
        )

        // Map sessions and steps
        val sessions = request.sessions.map { toSession(it, gameTactic) }
        sessions.forEach { it.gameTactics.add(gameTactic) }
        gameTactic.sessions.addAll(sessions)

        val saved = gameTacticRepository.save(gameTactic)

        return loadFullGameTactic(saved)
    }

    // ---------------- UPDATE ----------------
    @Transactional
    fun updateGameTactic(userId: Int, gameTacticId: Int, request: GameTacticRequest): GameTacticResponse {
        val gameTactic = gameTacticRepository.findById(gameTacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        if (gameTactic.owner?.id != userId) throw UnauthorizedException("Not allowed")

        gameTactic.name = request.name
        gameTactic.description = request.description
        gameTactic.is_premade = request.isPremade

        // Clear old sessions
        gameTactic.sessions.forEach { it.gameTactics.remove(gameTactic) }
        gameTactic.sessions.clear()

        // Add new sessions
        val newSessions = request.sessions.map { toSession(it, gameTactic) }
        newSessions.forEach { it.gameTactics.add(gameTactic) }
        gameTactic.sessions.addAll(newSessions)

        val updated = gameTacticRepository.save(gameTactic)
        return loadFullGameTactic(updated)
    }

    // ---------------- DELETE ----------------
    @Transactional
    fun deleteGameTactic(userId: Int, gameTacticId: Int) {
        val gameTactic = gameTacticRepository.findById(gameTacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        if (gameTactic.owner?.id != userId) throw UnauthorizedException("Not allowed")

        gameTacticRepository.delete(gameTactic)
    }

    // ---------------- GET ----------------
    @Transactional(readOnly = true)
    fun getGameTacticsByUserId(userId: Int): List<GameTacticResponse> {
        val tactics = gameTacticRepository.findByOwnerId(userId)
        return tactics.map { loadFullGameTactic(it) }
    }

    @Transactional(readOnly = true)
    fun getGameTacticById(gameTacticId: Int, userId: Int): GameTacticResponse {
        val gameTactic = gameTacticRepository.findById(gameTacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        if (gameTactic.owner?.id != userId) throw UnauthorizedException("Not allowed")

        return loadFullGameTactic(gameTactic)
    }

    // ---------------- HELPERS ----------------
    private fun toSession(req: SessionRequest, gameTactic: GameTactic): Session {
        val owner = gameTactic.owner ?: throw IllegalStateException("GameTactic owner must not be null")
        val session = Session(
            name = req.name,
            description = req.description,
            owner = owner
        )
        session.steps.addAll(req.steps.map { toStep(it, session, owner) })
        return session
    }

    private fun toStep(req: StepRequest, session: Session, user: User): Step {
        val step = Step(session = session)

        // Map players
        step.players.addAll(req.players.map { p ->
            val team = p.teamName?.let { name ->
                teamRepository.findByOwnerIdAndName(user.id, name)
                    ?: teamRepository.save(Team(name = name, color = p.color, owner = user))
            }
            Player(
                x = p.x,
                y = p.y,
                number = p.number,
                color = p.color,
                team = team
            )
        })

        // Map balls & goals
        step.balls.addAll(req.balls.map { Ball(x = it.x, y = it.y, color = it.color) })
        step.goals.addAll(req.goals.map { Goal(x = it.x, y = it.y, width = it.width, depth = it.depth, color = it.color) })

        // Map teams
        req.teams.forEach { t ->
            val team = teamRepository.findByOwnerIdAndName(user.id, t.name)
                ?: teamRepository.save(Team(name = t.name, color = t.color, owner = user))
            step.teams.add(team)
        }

        // Map formations
        step.formations.addAll(req.formations.map { f ->
            Formation(
                name = f.name,
                positions = f.positions.toFormationPositions(user, teamRepository)
            )
        })

        // Map cones
        step.cones.addAll(req.cones.map { Cone(x = it.x, y = it.y, color = it.color) })

        return step
    }

    // ---------------- FULL FETCH ----------------
    private fun loadFullGameTactic(gameTactic: GameTactic): GameTacticResponse {
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
