package org.sportstechsolutions.apitacticsapp.dtos

import org.slf4j.LoggerFactory
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.TeamRepository
import org.springframework.stereotype.Component

@Component
class EntityMappers(private val teamRepository: TeamRepository) {

    private val log = LoggerFactory.getLogger(EntityMappers::class.java)

    // =========================================================================
    // 1. REQUEST -> ENTITY (Creation / Updates)
    // =========================================================================

    fun toSession(req: SessionRequest, owner: User): Session {
        require(!req.name.isNullOrBlank()) { "Session name is required when creating a new session" }
        require(!req.description.isNullOrBlank()) { "Session description is required when creating a new session" }

        log.debug("Mapping SessionRequest to Session Entity for Owner ID: ${owner.id}")

        val session = Session(
            name = req.name,
            description = req.description,
            owner = owner,
            isPremade = req.isPremade,
            isPublic = req.isPublic,
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
            Formation(step = step, name = f.name, positions = toFormationPositions(f.positions, user))
        })

        return step
    }

    private fun toFormationPositions(requests: List<FormationPositionRequest>, user: User): MutableList<FormationPosition> {
        return requests.map { req ->
            val team = if (!req.teamName.isNullOrBlank()) {
                teamRepository.findByOwnerIdAndName(user.id, req.teamName)
                    ?: teamRepository.save(Team(name = req.teamName, color = req.teamColor ?: "white", owner = user))
            } else {
                null
            }
            FormationPosition(x = req.x, y = req.y, team = team)
        }.toMutableList()
    }

    // =========================================================================
    // 2. ENTITY -> SUMMARY RESPONSE (For Lists/Search/Tabs)
    // =========================================================================

    fun toSessionSummary(session: Session, role: AccessRole, currentUserId: Int): SessionSummaryResponse {
        return SessionSummaryResponse(
            id = session.id ?: 0,
            name = session.name ?: "",
            description = session.description ?: "",
            isPremade = session.isPremade,
            isPublic = session.isPublic,
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

    fun toPracticeSummary(practice: Practice, role: AccessRole, currentUserId: Int): PracticeSummaryResponse {
        return PracticeSummaryResponse(
            id = practice.id ?: 0,
            name = practice.name ?: "",
            description = practice.description ?: "",
            isPremade = practice.isPremade,
            isPublic = practice.isPublic,
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

    fun toGameTacticSummary(gameTactic: GameTactic, role: AccessRole, currentUserId: Int): GameTacticSummaryResponse {
        return GameTacticSummaryResponse(
            id = gameTactic.id ?: 0,
            name = gameTactic.name ?: "",
            description = gameTactic.description ?: "",
            isPremade = gameTactic.isPremade,
            isPublic = gameTactic.isPublic,
            ownerId = gameTactic.owner?.id ?: 0,
            sessions = gameTactic.sessions.map { toSessionSummary(it, AccessRole.NONE, currentUserId) },
            role = role,
            viewCount = gameTactic.viewCount,
            isFavorite = gameTactic.favoritedByUsers.any { it.id == currentUserId }
        )
    }

    // =========================================================================
    // 3. ENTITY -> FULL RESPONSE (For specific Item fetch)
    // =========================================================================

    fun loadFullSession(session: Session, role: AccessRole = AccessRole.OWNER, currentUserId: Int): SessionResponse {
        log.debug("Deep loading Full Session Response for ID: ${session.id}")

        // Trigger Lazy Loading for Steps
        session.steps.forEach { step ->
            step.players.size; step.balls.size; step.goals.size; step.teams.size; step.cones.size
            step.formations.forEach { it.positions.size }
        }

        return SessionResponse(
            id = session.id ?: 0,
            name = session.name ?: "",
            description = session.description ?: "",
            isPremade = session.isPremade,
            isPublic = session.isPublic,
            ownerId = session.owner?.id ?: 0,
            role = role,
            steps = session.steps.map { toStepResponse(it) },

            // AVOID CIRCULAR REFERENCE: Map parent IDs only
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

    fun loadFullPractice(practice: Practice, role: AccessRole = AccessRole.OWNER, currentUserId: Int): PracticeResponse {
        log.debug("Deep loading Full Practice Response for ID: ${practice.id}")

        return PracticeResponse(
            id = practice.id ?: 0,
            name = practice.name ?: "",
            description = practice.description ?: "",
            isPremade = practice.isPremade,
            isPublic = practice.isPublic,
            ownerId = practice.owner?.id ?: 0,
            role = role,
            // Recursively deep-load child sessions, but strictly override role to NONE to prevent UI leaks
            sessions = practice.sessions.map { loadFullSession(it, AccessRole.NONE, currentUserId) },
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

    fun loadFullGameTactic(gameTactic: GameTactic, role: AccessRole = AccessRole.OWNER, currentUserId: Int): GameTacticResponse {
        log.debug("Deep loading Full Game Tactic Response for ID: ${gameTactic.id}")

        return GameTacticResponse(
            id = gameTactic.id ?: 0,
            name = gameTactic.name ?: "",
            description = gameTactic.description ?: "",
            isPremade = gameTactic.isPremade,
            isPublic = gameTactic.isPublic,
            ownerId = gameTactic.owner?.id ?: 0,
            role = role,
            // Recursively deep-load child sessions, but strictly override role to NONE to prevent UI leaks
            sessions = gameTactic.sessions.map { loadFullSession(it, AccessRole.NONE, currentUserId) },
            viewCount = gameTactic.viewCount,
            isFavorite = gameTactic.favoritedByUsers.any { it.id == currentUserId }
        )
    }

    // --- Helper for Canvas Steps ---
    private fun toStepResponse(step: Step): StepResponse {
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