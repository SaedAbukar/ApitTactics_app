package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.ConflictException
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.*
import org.sportstechsolutions.apitacticsapp.repository.specifications.SearchSpecifications
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SessionService(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val userSessionAccessRepository: UserSessionAccessRepository,
    private val groupSessionAccessRepository: GroupSessionAccessRepository,
    private val entityMappers: EntityMappers
) {

    @Transactional(readOnly = true)
    fun getSessionsForTabs(userId: Int, pageable: Pageable): TabbedResponse<SessionSummaryResponse> {
        val personalPage = sessionRepository.findByOwnerId(userId, pageable)
        val personalPaged = PagedResponse(
            content = personalPage.content.map { entityMappers.toSessionSummary(it, AccessRole.OWNER, userId) },
            pageNumber = personalPage.number,
            pageSize = personalPage.size,
            totalElements = personalPage.totalElements,
            totalPages = personalPage.totalPages,
            isLast = personalPage.isLast
        )

        // DB level filter prevents pagination count mismatch
        val userSharedPage = userSessionAccessRepository.findByUserIdAndRoleNot(userId, AccessRole.NONE, pageable)
        val userSharedPaged = PagedResponse(
            content = userSharedPage.content.mapNotNull { access ->
                access.session?.let { s -> entityMappers.toSessionSummary(s, access.role, userId) }
            },
            pageNumber = userSharedPage.number,
            pageSize = userSharedPage.size,
            totalElements = userSharedPage.totalElements,
            totalPages = userSharedPage.totalPages,
            isLast = userSharedPage.isLast
        )

        val groupSharedPage = groupSessionAccessRepository.findByGroupMemberId(userId, pageable)
        val groupSharedPaged = PagedResponse(
            content = groupSharedPage.content.mapNotNull { access ->
                access.session?.let { s -> entityMappers.toSessionSummary(s, access.role, userId) }
            },
            pageNumber = groupSharedPage.number,
            pageSize = groupSharedPage.size,
            totalElements = groupSharedPage.totalElements,
            totalPages = groupSharedPage.totalPages,
            isLast = groupSharedPage.isLast
        )

        return TabbedResponse(personalPaged, userSharedPaged, groupSharedPaged)
    }

    @Transactional(readOnly = true)
    fun searchSessions(userId: Int, request: SessionSearchRequest, pageable: Pageable): PagedResponse<SessionSummaryResponse> {
        // Build Spec: accessibleIds memory array replaced with DB Exists logic
        val spec = SearchSpecifications.buildSessionSearchSpec(request, userId)

        val finalPageable = if (request.sortBy == SortBy.VIEWS) {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "viewCount"))
        } else {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "id"))
        }

        val sessionPage = sessionRepository.findAll(spec, finalPageable)
        val sessionIds = sessionPage.content.mapNotNull { it.id }

        // Bulk fetch roles to prevent N+1 queries during DTO mapping
        val userRoleMap = if (userId != 0 && sessionIds.isNotEmpty()) {
            userSessionAccessRepository.findRolesForUserInSessions(userId, sessionIds).associate { it.sessionId to it.role }
        } else emptyMap()

        val groupRoleMap = if (userId != 0 && sessionIds.isNotEmpty()) {
            groupSessionAccessRepository.findRolesForUserInSessions(userId, sessionIds).associate { it.sessionId to it.role }
        } else emptyMap()

        val content = sessionPage.content.map { session ->
            val sessionId = session.id ?: 0

            val role = when {
                userId == 0 -> AccessRole.VIEWER
                session.owner?.id == userId -> AccessRole.OWNER
                userRoleMap.containsKey(sessionId) -> userRoleMap[sessionId]!!
                groupRoleMap.containsKey(sessionId) -> groupRoleMap[sessionId]!!
                session.isPublic || session.isPremade -> AccessRole.VIEWER
                else -> AccessRole.NONE
            }
            entityMappers.toSessionSummary(session, role, userId)
        }

        return PagedResponse(
            content = content,
            pageNumber = sessionPage.number,
            pageSize = sessionPage.size,
            totalElements = sessionPage.totalElements,
            totalPages = sessionPage.totalPages,
            isLast = sessionPage.isLast
        )
    }

    @Transactional
    fun createSession(userId: Int, request: SessionRequest): SessionResponse {
        val user = userRepository.findById(userId).orElseThrow { ResourceNotFoundException("User not found") }
        val session = entityMappers.toSession(request, user)
        val saved = sessionRepository.save(session)
        return entityMappers.loadFullSession(saved, AccessRole.OWNER, userId)
    }

    @Transactional
    fun updateSession(userId: Int, sessionId: Int, request: SessionRequest, groupId: Int? = null): SessionResponse {
        val session = sessionRepository.findById(sessionId).orElseThrow { ResourceNotFoundException("Session not found") }

        val role = getUserRoleForSession(userId, session, groupId)
        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to edit this session")
        if (request.name.isNullOrBlank()) throw IllegalArgumentException("Session name is required for updates")

        val owner = session.owner ?: throw IllegalStateException("Session must have an owner")

        session.name = request.name
        session.description = request.description ?: ""
        session.isPremade = request.isPremade
        session.isPublic = request.isPublic
        session.phaseOfPlay = request.phaseOfPlay
        session.ballContext = request.ballContext
        session.drillFormat = request.drillFormat
        session.minPlayers = request.minPlayers
        session.maxPlayers = request.maxPlayers
        session.durationMinutes = request.durationMinutes
        session.areaSize = request.areaSize
        session.targetAgeLevel = request.targetAgeLevel

        session.tacticalActions.clear()
        session.tacticalActions.addAll(request.tacticalActions)

        session.qualityMakers.clear()
        session.qualityMakers.addAll(request.qualityMakers)

        session.steps.clear()
        session.steps.addAll(request.steps.map { entityMappers.toStep(it, session, owner) })

        val updated = sessionRepository.save(session)
        return entityMappers.loadFullSession(updated, role, userId)
    }

    @Transactional
    fun deleteSession(userId: Int, sessionId: Int, groupId: Int? = null) {
        val session = sessionRepository.findById(sessionId).orElseThrow { ResourceNotFoundException("Session not found") }

        val role = getUserRoleForSession(userId, session, groupId)
        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to delete this session")

        if (session.practices.isNotEmpty() || session.gameTactics.isNotEmpty()) {
            throw ConflictException(
                "Cannot delete session: It is currently used in ${session.practices.size} Practice(s) and ${session.gameTactics.size} Game Tactic(s). " +
                        "Remove it from these containers first to maintain data integrity."
            )
        }

        userSessionAccessRepository.deleteAllBySession(session)
        sessionRepository.delete(session)
    }

    @Transactional
    fun getSessionById(sessionId: Int, userId: Int, groupId: Int? = null): SessionResponse {
        val session = sessionRepository.findById(sessionId).orElseThrow { ResourceNotFoundException("Session not found") }

        val role = getUserRoleForSession(userId, session, groupId)
        if (role == AccessRole.NONE) throw UnauthorizedException("You do not have access to this session")

        session.viewCount += 1
        sessionRepository.save(session)

        return entityMappers.loadFullSession(session, role, userId)
    }

    @Transactional
    fun toggleFavorite(userId: Int, sessionId: Int): Boolean {
        val role = getUserRoleForSession(userId, sessionId)
        if (role == AccessRole.NONE) throw UnauthorizedException("You do not have access to this session")

        val isAlreadyFavorite = sessionRepository.isSessionFavoritedByUser(sessionId, userId)
        if (isAlreadyFavorite) {
            sessionRepository.removeFavorite(sessionId, userId)
        } else {
            sessionRepository.addFavorite(sessionId, userId)
        }
        return !isAlreadyFavorite
    }

    // Main resolver: operates on the entity to prevent double fetching
    fun getUserRoleForSession(userId: Int, session: Session, groupId: Int? = null): AccessRole {
        val sessionId = session.id ?: return AccessRole.NONE
        if (session.owner?.id == userId) return AccessRole.OWNER

        val userAccess = userSessionAccessRepository.findByUserIdAndSessionId(userId, sessionId)
        if (userAccess != null) return userAccess.role

        val groupRole = groupSessionAccessRepository.findRoleForUserInSession(userId, sessionId)
        if (groupRole != null) return groupRole

        return if (session.isPublic || session.isPremade) AccessRole.VIEWER else AccessRole.NONE
    }

    // Fallback resolver
    fun getUserRoleForSession(userId: Int, sessionId: Int, groupId: Int? = null): AccessRole {
        val session = sessionRepository.findById(sessionId).orElseThrow { ResourceNotFoundException("Session not found") }
        return getUserRoleForSession(userId, session, groupId)
    }
}