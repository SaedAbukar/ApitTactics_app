package org.sportstechsolutions.apitacticsapp.dtos

import org.sportstechsolutions.apitacticsapp.mappers.*
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.TeamRepository
import org.springframework.stereotype.Component

@Component
class EntityMappers(private val teamRepository: TeamRepository) {

    // ------------------------------------------------------------
    // SESSION CREATION (Request -> Entity)
    // ------------------------------------------------------------
    fun toSession(req: SessionRequest, owner: User): Session {
        if (req.name.isNullOrBlank()) {
            throw IllegalArgumentException("Session name is required when creating a new session")
        }
        if (req.description.isNullOrBlank()) {
            throw IllegalArgumentException("Session description is required when creating a new session")
        }

        val session = Session(
            name = req.name,
            description = req.description,
            owner = owner,
            isPremade = req.isPremade,
            isPublic = req.isPublic, // Map isPublic from request
            phaseOfPlay = req.phaseOfPlay,
            ballContext = req.ballContext,
            drillFormat = req.drillFormat,
            minPlayers = req.minPlayers,
            maxPlayers = req.maxPlayers,
            durationMinutes = req.durationMinutes,
            areaSize = req.areaSize,
            targetAgeLevel = req.targetAgeLevel,
            tacticalActions = req.tacticalActions.toMutableSet(),
            qualityMakers = req.qualityMakers.toMutableSet()
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

        step.players.addAll(req.players.map { p ->
            val team = if (!p.teamName.isNullOrBlank()) {
                teamRepository.findByOwnerIdAndName(user.id, p.teamName)
                    ?: teamRepository.save(Team(name = p.teamName, color = p.color, owner = user))
            } else {
                null
            }
            Player(step = step, x = p.x, y = p.y, number = p.number, color = p.color, team = team)
        })

        step.balls.addAll(req.balls.map { Ball(step = step, x = it.x, y = it.y, color = it.color) })

        step.goals.addAll(req.goals.map {
            Goal(
                step = step,
                x = it.x,
                y = it.y,
                width = it.width,
                depth = it.depth,
                color = it.color,
                rotation = it.rotation
            )
        })

        step.cones.addAll(req.cones.map { Cone(step = step, x = it.x, y = it.y, color = it.color) })

        req.teams.forEach { t ->
            val team = teamRepository.findByOwnerIdAndName(user.id, t.name)
                ?: teamRepository.save(Team(name = t.name, color = t.color, owner = user))
            step.teams.add(team)
        }

        step.formations.addAll(req.formations.map { f ->
            Formation(step = step, name = f.name, positions = f.positions.toFormationPositions(user, teamRepository))
        })

        return step
    }

    // ------------------------------------------------------------
    // RESPONSE MAPPING (Entity -> DTO)
    // ------------------------------------------------------------

    // SESSION SUMMARY
    fun toSessionSummary(session: Session, role: AccessRole, currentUserId: Int): SessionSummaryResponse {
        return SessionSummaryResponse(
            id = session.id ?: 0,
            name = session.name ?: "",
            description = session.description ?: "",
            isPremade = session.isPremade,
            isPublic = session.isPublic, // Map isPublic to response
            ownerId = session.owner?.id ?: 0,
            stepCount = session.steps.size,
            role = role,

            practiceIds = session.practices.mapNotNull { it.id },
            gameTacticIds = session.gameTactics.mapNotNull { it.id },

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

            viewCount = session.viewCount,
            isFavorite = session.favoritedByUsers.any { it.id == currentUserId }
        )
    }

    // FULL SESSION
    fun loadFullSession(session: Session, role: AccessRole = AccessRole.OWNER, currentUserId: Int): SessionResponse {
        session.steps.forEach { step ->
            step.players.size; step.balls.size; step.goals.size; step.teams.size; step.cones.size
            step.formations.forEach { it.positions.size }
        }
        session.practices.size
        session.gameTactics.size

        val baseResponse = SessionMapper.toSessionResponse(session, currentUserId)
        return baseResponse.copy(role = role)
    }

    // PRACTICE SUMMARY
    fun toPracticeSummary(practice: Practice, role: AccessRole, currentUserId: Int): PracticeSummaryResponse {
        return PracticeSummaryResponse(
            id = practice.id ?: 0,
            name = practice.name ?: "",
            description = practice.description ?: "",
            isPremade = practice.isPremade,
            isPublic = practice.isPublic, // Map isPublic to response
            ownerId = practice.owner?.id ?: 0,
            sessions = practice.sessions.map { toSessionSummary(it, AccessRole.NONE, currentUserId) },
            role = role,
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
            viewCount = practice.viewCount,
            isFavorite = practice.favoritedByUsers.any { it.id == currentUserId }
        )
    }

    // FULL PRACTICE
    fun loadFullPractice(practice: Practice, role: AccessRole = AccessRole.OWNER, currentUserId: Int): PracticeResponse {
        practice.sessions.forEach { session ->
            session.steps.forEach { step ->
                step.players.size; step.balls.size; step.goals.size; step.teams.size; step.cones.size
                step.formations.forEach { it.positions.size }
            }
            session.practices.size
            session.gameTactics.size
        }

        val baseResponse = PracticeMapper.toPracticeResponse(practice, currentUserId)
        return baseResponse.copy(role = role, ownerId = practice.owner?.id ?: 0)
    }

    // GAME TACTIC SUMMARY
    fun toGameTacticSummary(gameTactic: GameTactic, role: AccessRole, currentUserId: Int): GameTacticSummaryResponse {
        return GameTacticSummaryResponse(
            id = gameTactic.id ?: 0,
            name = gameTactic.name ?: "",
            description = gameTactic.description ?: "",
            isPremade = gameTactic.isPremade,
            isPublic = gameTactic.isPublic, // Map isPublic to response
            ownerId = gameTactic.owner?.id ?: 0,
            sessions = gameTactic.sessions.map { toSessionSummary(it, AccessRole.NONE, currentUserId) },
            role = role,
            viewCount = gameTactic.viewCount,
            isFavorite = gameTactic.favoritedByUsers.any { it.id == currentUserId }
        )
    }

    // FULL GAME TACTIC
    fun loadFullGameTactic(gameTactic: GameTactic, role: AccessRole = AccessRole.OWNER, currentUserId: Int): GameTacticResponse {
        gameTactic.sessions.forEach { session ->
            session.steps.forEach { step ->
                step.players.size; step.balls.size; step.goals.size; step.teams.size; step.cones.size
                step.formations.forEach { it.positions.size }
            }
            session.practices.size
            session.gameTactics.size
        }
        val baseResponse = GameTacticMapper.toGameTacticResponse(gameTactic, currentUserId)
        return baseResponse.copy(role = role, ownerId = gameTactic.owner?.id ?: 0)
    }
}