package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.mappers.SessionMapper
import org.sportstechsolutions.apitacticsapp.mappers.toFormationPositions
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.SessionRepository
import org.sportstechsolutions.apitacticsapp.repository.TeamRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SessionService(
    private val sessionRepository: SessionRepository,
    private val teamRepository: TeamRepository
) {

    // CREATE
    @Transactional
    fun createSession(userId: Int, request: SessionRequest): SessionResponse {
        val owner = User(id = userId)

        val session = Session(
            name = request.name,
            description = request.description,
            owner = owner
        )
        session.steps.addAll(request.steps.map { toStep(it, session, owner) })

        val saved = sessionRepository.save(session)
        return loadFullSession(saved)
    }

    // UPDATE
    @Transactional
    fun updateSession(userId: Int, sessionId: Int, request: SessionRequest): SessionResponse {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        val owner = session.owner
            ?: throw IllegalStateException("Session must have an owner")

        if (owner.id != userId) throw UnauthorizedException("Not allowed")

        session.name = request.name
        session.description = request.description
        session.steps.clear()
        session.steps.addAll(request.steps.map { toStep(it, session, owner) })

        val updated = sessionRepository.save(session)
        return loadFullSession(updated)
    }

    // DELETE
    @Transactional
    fun deleteSession(userId: Int, sessionId: Int) {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        val owner = session.owner
            ?: throw IllegalStateException("Session must have an owner")

        if (owner.id != userId) throw UnauthorizedException("Not allowed")

        sessionRepository.delete(session)
    }

    // GET
    @Transactional(readOnly = true)
    fun getSessionsByUserId(userId: Int): List<SessionResponse> {
        return sessionRepository.findByOwnerId(userId).map { loadFullSession(it) }
    }

    @Transactional(readOnly = true)
    fun getSessionById(sessionId: Int, userId: Int): SessionResponse {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        val owner = session.owner
            ?: throw IllegalStateException("Session must have an owner")

        if (owner.id != userId) throw UnauthorizedException("Not allowed")

        return loadFullSession(session)
    }

    // HELPERS
    private fun toStep(req: StepRequest, session: Session, user: User): Step {
        val step = Step(session = session)

        step.players.addAll(req.players.map { p ->
            val team = p.teamName?.let { name ->
                teamRepository.findByOwnerIdAndName(user.id, name)
                    ?: teamRepository.save(Team(name = name, color = p.color, owner = user))
            }
            Player(x = p.x, y = p.y, number = p.number, color = p.color, team = team)
        })

        step.balls.addAll(req.balls.map { Ball(x = it.x, y = it.y, color = it.color) })
        step.goals.addAll(req.goals.map { Goal(x = it.x, y = it.y, width = it.width, depth = it.depth, color = it.color) })
        req.teams.forEach { t ->
            val team = teamRepository.findByOwnerIdAndName(user.id, t.name)
                ?: teamRepository.save(Team(name = t.name, color = t.color, owner = user))
            step.teams.add(team)
        }
        step.formations.addAll(req.formations.map { f ->
            Formation(
                name = f.name,
                positions = f.positions.toFormationPositions(user, teamRepository)
            )
        })
        step.cones.addAll(req.cones.map { Cone(x = it.x, y = it.y, color = it.color) })

        return step
    }

    private fun loadFullSession(session: Session): SessionResponse {
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
}
