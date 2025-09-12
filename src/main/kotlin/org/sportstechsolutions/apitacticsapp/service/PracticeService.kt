package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.mappers.PracticeMapper
import org.sportstechsolutions.apitacticsapp.mappers.toFormationPositions
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PracticeService(
    private val practiceRepository: PracticeRepository
) {

    // ---------------- CREATE ----------------
    @Transactional
    fun createPractice(userId: Int, request: PracticeRequest): Practice {
        val practice = Practice(
            name = request.name,
            description = request.description,
            is_premade = request.isPremade,
            owner = User(id = userId)
        )
        practice.sessions.addAll(request.sessions.map { toSession(it, practice) })
        return practiceRepository.save(practice)
    }

    // ---------------- UPDATE ----------------
    @Transactional
    fun updatePractice(userId: Int, practiceId: Int, request: PracticeRequest): Practice {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        if (practice.owner?.id != userId) throw UnauthorizedException("Not allowed")

        practice.name = request.name
        practice.description = request.description
        practice.is_premade = request.isPremade

        practice.sessions.clear()
        practice.sessions.addAll(request.sessions.map { toSession(it, practice) })

        return practiceRepository.save(practice)
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
    fun getPracticesByUserId(userId: Int): List<PracticeResponse> =
        practiceRepository.findByOwnerId(userId).map { PracticeMapper.toPracticeResponse(it) }

    // ---------------- HELPERS ----------------
    private fun toSession(req: SessionRequest, practice: Practice): Session {
        val session = Session(name = req.name, description = req.description, owner = practice.owner)
        session.practices.add(practice)
        session.steps.addAll(req.steps.map { toStep(it, session) })
        return session
    }

    private fun toStep(req: StepRequest, session: Session): Step {
        val step = Step(session = session)
        step.players.addAll(req.players.map { p ->
            Player(x = p.x, y = p.y, number = p.number, color = p.color, team = p.teamId?.let { Team(id = it) })
        })
        step.balls.addAll(req.balls.map { Ball(x = it.x, y = it.y, color = it.color) })
        step.goals.addAll(req.goals.map { Goal(x = it.x, y = it.y, width = it.width, depth = it.depth, color = it.color) })
        step.teams.addAll(req.teams.map { t -> Team(id = t.id ?: 0, name = t.name, color = t.color) })
        step.formations.addAll(req.formations.map { f ->
            Formation(name = f.name, positions = f.positions.toFormationPositions())
        })
        step.cones.addAll(req.cones.map { Cone(x = it.x, y = it.y, color = it.color) })
        return step
    }
}
