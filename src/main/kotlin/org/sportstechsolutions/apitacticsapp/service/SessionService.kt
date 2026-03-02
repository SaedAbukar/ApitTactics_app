package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.ConflictException
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.*
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

    // ------------------------------------------------------------
    // Tabbed view (Returns Paginated Summaries)
    // ------------------------------------------------------------
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

        val userSharedPage = userSessionAccessRepository.findByUserId(userId, pageable)
        val userSharedPaged = PagedResponse(
            content = userSharedPage.content.filter { it.role != AccessRole.NONE }.mapNotNull { access ->
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

    // ------------------------------------------------------------
    // Advanced Search
    // ------------------------------------------------------------
    @Transactional(readOnly = true)
    fun searchSessions(userId: Int, request: SessionSearchRequest, pageable: Pageable): PagedResponse<SessionSummaryResponse> {
        // 1. Guest Safety: Guests have no "accessible" private IDs
        val accessibleIds = if (userId == 0) emptySet() else sessionRepository.findAllAccessibleSessionIds(userId)

        val spec = org.sportstechsolutions.apitacticsapp.repository.specifications.SearchSpecifications
            .buildSessionSearchSpec(request, accessibleIds, userId)

        val finalPageable = if (request.sortBy == SortBy.VIEWS) {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "viewCount"))
        } else {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "id"))
        }

        val sessionPage = sessionRepository.findAll(spec, finalPageable)

        val content = sessionPage.content.map { session ->
            // 2. Role Resolution: Guests are always VIEWERS
            val role = if (userId == 0) AccessRole.VIEWER else getUserRoleForSession(userId, session.id ?: 0)
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

    // ------------------------------------------------------------
    // CRUD Operations
    // ------------------------------------------------------------
    @Transactional
    fun createSession(userId: Int, request: SessionRequest): SessionResponse {
        val user = userRepository.findById(userId).orElseThrow { ResourceNotFoundException("User not found") }

        // entityMappers.toSession already handles the isPublic flag
        val session = entityMappers.toSession(request, user)
        val saved = sessionRepository.save(session)

        return entityMappers.loadFullSession(saved, AccessRole.OWNER, userId)
    }

    @Transactional
    fun updateSession(userId: Int, sessionId: Int, request: SessionRequest, groupId: Int? = null): SessionResponse {
        val session = sessionRepository.findById(sessionId).orElseThrow { ResourceNotFoundException("Session not found") }

        val role = if (session.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForSession(userId, sessionId, groupId)

        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to edit this session")

        if (request.name.isNullOrBlank()) {
            throw IllegalArgumentException("Session name is required for updates")
        }

        val owner = session.owner ?: throw IllegalStateException("Session must have an owner")

        // Update basic fields
        session.name = request.name
        session.description = request.description ?: ""
        session.isPremade = request.isPremade
        session.isPublic = request.isPublic // Added visibility update logic

        // Taxonomy
        session.phaseOfPlay = request.phaseOfPlay
        session.ballContext = request.ballContext
        session.drillFormat = request.drillFormat
        session.minPlayers = request.minPlayers
        session.maxPlayers = request.maxPlayers
        session.durationMinutes = request.durationMinutes
        session.areaSize = request.areaSize
        session.targetAgeLevel = request.targetAgeLevel

        // Collections
        session.tacticalActions.clear()
        session.tacticalActions.addAll(request.tacticalActions)

        session.qualityMakers.clear()
        session.qualityMakers.addAll(request.qualityMakers)

        // Steps (Canvas elements)
        session.steps.clear()
        session.steps.addAll(request.steps.map { entityMappers.toStep(it, session, owner) })

        val updated = sessionRepository.save(session)
        return entityMappers.loadFullSession(updated, role, userId)
    }

    @Transactional
    fun deleteSession(userId: Int, sessionId: Int, groupId: Int? = null) {
        val session = sessionRepository.findById(sessionId).orElseThrow { ResourceNotFoundException("Session not found") }

        val role = if (session.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForSession(userId, sessionId, groupId)

        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to delete this session")

        // >>> BUSINESS RULE ENFORCEMENT <<<
        // We check bidirectional links. If these lists aren't empty, someone else is using this drill.
        if (session.practices.isNotEmpty() || session.gameTactics.isNotEmpty()) {
            val pCount = session.practices.size
            val gCount = session.gameTactics.size
            throw ConflictException(
                "Cannot delete session: It is currently used in $pCount Practice(s) and $gCount Game Tactic(s). " +
                        "Remove it from these containers first to maintain data integrity."
            )
        }

        userSessionAccessRepository.deleteAllBySession(session)
        sessionRepository.delete(session)
    }

    @Transactional
    fun getSessionById(sessionId: Int, userId: Int, groupId: Int? = null): SessionResponse {
        val session = sessionRepository.findById(sessionId).orElseThrow { ResourceNotFoundException("Session not found") }

        val role = if (session.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForSession(userId, sessionId, groupId)

        if (role == AccessRole.NONE) throw UnauthorizedException("You do not have access to this session")

        // Increment View Count for engagement analytics
        session.viewCount += 1
        sessionRepository.save(session)

        return entityMappers.loadFullSession(session, role, userId)
    }

    // ------------------------------------------------------------
    // Favorites & Accessibility
    // ------------------------------------------------------------
    @Transactional
    fun toggleFavorite(userId: Int, sessionId: Int): Boolean {
        val session = sessionRepository.findById(sessionId).orElseThrow { ResourceNotFoundException("Session not found") }
        val user = userRepository.findById(userId).orElseThrow { ResourceNotFoundException("User not found") }

        val role = if (session.owner?.id == userId) AccessRole.OWNER else getUserRoleForSession(userId, sessionId)
        if (role == AccessRole.NONE) throw UnauthorizedException("You do not have access to this session")

        val isAlreadyFavorite = session.favoritedByUsers.any { it.id == userId }
        if (isAlreadyFavorite) {
            session.favoritedByUsers.removeIf { it.id == userId }
        } else {
            session.favoritedByUsers.add(user)
        }

        sessionRepository.save(session)
        return !isAlreadyFavorite
    }

    // ------------------------------------------------------------
    // Access role resolver
    // ------------------------------------------------------------
    fun getUserRoleForSession(userId: Int, sessionId: Int, groupId: Int? = null): AccessRole {
        val session = sessionRepository.findById(sessionId).orElseThrow { ResourceNotFoundException("Session not found") }

        // 1. Owner Check
        if (session.owner?.id == userId) return AccessRole.OWNER

        // 2. Direct User Share Check
        val userAccess = userSessionAccessRepository.findByUserIdAndSessionId(userId, sessionId)
        if (userAccess != null) return userAccess.role

        // 3. Group Share Check
        val groupAccess = if (groupId != null) {
            groupSessionAccessRepository.findBySessionIdAndGroupId(sessionId, groupId)
                ?.takeIf { it.group?.members?.any { m -> m.id == userId } == true }
        } else {
            groupSessionAccessRepository.findBySessionId(sessionId)
                .firstOrNull { it.group?.members?.any { m -> m.id == userId } == true }
        }
        if (groupAccess != null) return groupAccess.role

        // 4. Global Visibility Check (Public/Premade)
        return if (session.isPublic || session.isPremade) AccessRole.VIEWER else AccessRole.NONE
    }
}