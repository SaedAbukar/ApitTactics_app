package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.mappers.PracticeMapper
import org.sportstechsolutions.apitacticsapp.mappers.toFormationPositions
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.PracticeRepository
import org.sportstechsolutions.apitacticsapp.repository.TeamRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PracticeService(
    private val practiceRepository: PracticeRepository,
    private val teamRepository: TeamRepository
) {

    // ---------------- CREATE ----------------
    @Transactional
    fun createPractice(userId: Int, request: PracticeRequest): PracticeResponse {
        val practice = Practice(
            name = request.name,
            description = request.description,
            is_premade = request.isPremade,
            owner = User(id = userId)
        )

        // Map sessions and steps
        val sessions = request.sessions.map { toSession(it, practice) }
        sessions.forEach { it.practices.add(practice) }
        practice.sessions.addAll(sessions)

        val savedPractice = practiceRepository.save(practice)

        return loadFullPractice(savedPractice)
    }

    // ---------------- UPDATE ----------------
    @Transactional
    fun updatePractice(userId: Int, practiceId: Int, request: PracticeRequest): PracticeResponse {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        if (practice.owner?.id != userId) throw UnauthorizedException("Not allowed")

        practice.name = request.name
        practice.description = request.description
        practice.is_premade = request.isPremade

        // Clear old sessions
        practice.sessions.forEach { it.practices.remove(practice) }
        practice.sessions.clear()

        // Add new sessions
        val newSessions = request.sessions.map { toSession(it, practice) }
        newSessions.forEach { it.practices.add(practice) }
        practice.sessions.addAll(newSessions)

        val updated = practiceRepository.save(practice)
        return loadFullPractice(updated)
    }

    // ---------------- DELETE ----------------
    @Transactional
    fun deletePractice(userId: Int, practiceId: Int) {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        if (practice.owner?.id != userId) throw UnauthorizedException("Not allowed")

        practiceRepository.delete(practice)
    }

    // ---------------- GET ----------------
    @Transactional(readOnly = true)
    fun getPracticesByUserId(userId: Int): List<PracticeResponse> {
        val practices = practiceRepository.findByOwnerId(userId)
        return practices.map { loadFullPractice(it) }
    }

    @Transactional(readOnly = true)
    fun getPracticeById(practiceId: Int, userId: Int): PracticeResponse {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        if (practice.owner?.id != userId) throw UnauthorizedException("Not allowed")

        return loadFullPractice(practice)
    }

    // ---------------- HELPERS ----------------
    private fun toSession(req: SessionRequest, practice: Practice): Session {
        val owner = practice.owner ?: throw IllegalStateException("Practice owner must not be null")
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

        // Map teams (from StepRequest.teams)
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
    private fun loadFullPractice(practice: Practice): PracticeResponse {
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
}
