package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.*
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
class GameTacticService(
    private val gameTacticRepository: GameTacticRepository,
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val userGameTacticAccessRepository: UserGameTacticAccessRepository,
    private val groupGameTacticAccessRepository: GroupGameTacticAccessRepository,
    private val entityMappers: EntityMappers,
) {

    // ------------------------------------------------------------
    // Tabbed view (Returns Paginated Summaries)
    // ------------------------------------------------------------
    @Transactional(readOnly = true)
    fun getGameTacticsForTabs(userId: Int, pageable: Pageable): TabbedResponse<GameTacticSummaryResponse> {
        val personalPage = gameTacticRepository.findByOwnerId(userId, pageable)
        val personalPaged = PagedResponse(
            content = personalPage.content.map { entityMappers.toGameTacticSummary(it, AccessRole.OWNER, userId) },
            pageNumber = personalPage.number,
            pageSize = personalPage.size,
            totalElements = personalPage.totalElements,
            totalPages = personalPage.totalPages,
            isLast = personalPage.isLast
        )

        val userSharedPage = userGameTacticAccessRepository.findByUserId(userId, pageable)
        val userSharedPaged = PagedResponse(
            content = userSharedPage.content.filter { it.role != AccessRole.NONE }.mapNotNull { access ->
                access.gameTactic?.let { tactic -> entityMappers.toGameTacticSummary(tactic, access.role, userId) }
            },
            pageNumber = userSharedPage.number,
            pageSize = userSharedPage.size,
            totalElements = userSharedPage.totalElements,
            totalPages = userSharedPage.totalPages,
            isLast = userSharedPage.isLast
        )

        val groupSharedPage = groupGameTacticAccessRepository.findByGroupMemberId(userId, pageable)
        val groupSharedPaged = PagedResponse(
            content = groupSharedPage.content.mapNotNull { access ->
                access.gameTactic?.let { tactic -> entityMappers.toGameTacticSummary(tactic, access.role, userId) }
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
    fun searchGameTactics(userId: Int, request: GameTacticSearchRequest, pageable: Pageable): PagedResponse<GameTacticSummaryResponse> {
        val accessibleIds: Set<Int> = if (userId == 0) {
            emptySet()
        } else {
            gameTacticRepository.findAllAccessibleGameTacticIds(userId).toSet()
        }

        val spec = org.sportstechsolutions.apitacticsapp.repository.specifications.SearchSpecifications.buildGameTacticSearchSpec(request, accessibleIds, userId)

        val finalPageable = if (request.sortBy == SortBy.VIEWS) {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "viewCount"))
        } else {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "id"))
        }

        val tacticPage = gameTacticRepository.findAll(spec, finalPageable)
        val content = tacticPage.content.map { tactic ->
            val role = getUserRoleForGameTactic(userId, tactic.id ?: 0)
            entityMappers.toGameTacticSummary(tactic, role, userId)
        }

        return PagedResponse(
            content = content,
            pageNumber = tacticPage.number,
            pageSize = tacticPage.size,
            totalElements = tacticPage.totalElements,
            totalPages = tacticPage.totalPages,
            isLast = tacticPage.isLast
        )
    }

    // ------------------------------------------------------------
    // CRUD Operations
    // ------------------------------------------------------------
    @Transactional
    fun createGameTactic(userId: Int, request: GameTacticRequest): GameTacticResponse {
        val user = userRepository.findById(userId).orElseThrow { ResourceNotFoundException("User not found") }

        val gameTactic = GameTactic(
            name = request.name,
            description = request.description,
            isPremade = request.isPremade,
            isPublic = request.isPublic,
            owner = user
        )

        // FIXED: Strictly link existing sessions by ID to avoid JSON parse errors
        val sessionsToAttach = request.sessions.map { dto ->
            val sessionId = dto.id ?: throw IllegalArgumentException("Session ID required")
            sessionRepository.findById(sessionId)
                .orElseThrow { ResourceNotFoundException("Session $sessionId not found") }
        }

        // BIDIRECTIONAL SYNC: Ensure Sessions know they belong to this Tactic
        sessionsToAttach.forEach { session ->
            session.gameTactics.add(gameTactic)
        }
        gameTactic.sessions.addAll(sessionsToAttach)

        val saved = gameTacticRepository.save(gameTactic)
        return entityMappers.loadFullGameTactic(saved, AccessRole.OWNER, userId)
    }

    @Transactional
    fun updateGameTactic(userId: Int, gameTacticId: Int, request: GameTacticRequest, groupId: Int? = null): GameTacticResponse {
        val gameTactic = gameTacticRepository.findById(gameTacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        val role = if (gameTactic.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForGameTactic(userId, gameTacticId, groupId)

        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to edit this game tactic")

        gameTactic.name = request.name
        gameTactic.description = request.description
        gameTactic.isPremade = request.isPremade
        gameTactic.isPublic = request.isPublic

        // DETACH OLD SESSIONS FROM BOTH SIDES
        gameTactic.sessions.forEach { session ->
            session.gameTactics.remove(gameTactic)
        }
        gameTactic.sessions.clear()

        // FIXED: Use IDs only for attachment to prevent Constructor NPE
        val sessionsToAttach = request.sessions.map { dto ->
            val sessionId = dto.id ?: throw IllegalArgumentException("Session ID required")
            sessionRepository.findById(sessionId)
                .orElseThrow { ResourceNotFoundException("Session $sessionId not found") }
        }

        // SYNC: Re-attach new sessions to both sides
        sessionsToAttach.forEach { session ->
            session.gameTactics.add(gameTactic)
        }
        gameTactic.sessions.addAll(sessionsToAttach)

        val updated = gameTacticRepository.save(gameTactic)
        return entityMappers.loadFullGameTactic(updated, role, userId)
    }

    @Transactional
    fun deleteGameTactic(userId: Int, gameTacticId: Int, groupId: Int? = null) {
        val gameTactic = gameTacticRepository.findById(gameTacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        val role = if (gameTactic.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForGameTactic(userId, gameTacticId, groupId)

        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to delete this game tactic")

        // Cleanup: Remove the Tactic from the Sessions' internal lists
        gameTactic.sessions.forEach { it.gameTactics.remove(gameTactic) }

        userGameTacticAccessRepository.deleteAllByGameTactic(gameTactic)
        gameTacticRepository.delete(gameTactic)
    }

    @Transactional
    fun getGameTacticById(gameTacticId: Int, userId: Int, groupId: Int? = null): GameTacticResponse {
        val gameTactic = gameTacticRepository.findById(gameTacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        val role = if (gameTactic.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForGameTactic(userId, gameTacticId, groupId)

        if (role == AccessRole.NONE) throw UnauthorizedException("You do not have access to this game tactic")

        gameTactic.viewCount += 1
        gameTacticRepository.save(gameTactic)

        return entityMappers.loadFullGameTactic(gameTactic, role, userId)
    }

    // ------------------------------------------------------------
    // Favorites & Accessibility
    // ------------------------------------------------------------
    @Transactional
    fun toggleFavorite(userId: Int, gameTacticId: Int): Boolean {
        val gameTactic = gameTacticRepository.findById(gameTacticId).orElseThrow { ResourceNotFoundException("Game Tactic not found") }
        val user = userRepository.findById(userId).orElseThrow { ResourceNotFoundException("User not found") }

        val role = if (gameTactic.owner?.id == userId) AccessRole.OWNER else getUserRoleForGameTactic(userId, gameTacticId)
        if (role == AccessRole.NONE) throw UnauthorizedException("You do not have access to this game tactic")

        val isAlreadyFavorite = gameTactic.favoritedByUsers.any { it.id == userId }
        if (isAlreadyFavorite) {
            gameTactic.favoritedByUsers.removeIf { it.id == userId }
        } else {
            gameTactic.favoritedByUsers.add(user)
        }

        gameTacticRepository.save(gameTactic)
        return !isAlreadyFavorite
    }

    fun getUserRoleForGameTactic(userId: Int, gameTacticId: Int, groupId: Int? = null): AccessRole {
        val gameTactic = gameTacticRepository.findById(gameTacticId).orElseThrow { ResourceNotFoundException("Game tactic not found") }

        if (gameTactic.owner?.id == userId) return AccessRole.OWNER

        val userAccess = userGameTacticAccessRepository.findByUserIdAndGameTacticId(userId, gameTacticId)
        if (userAccess != null) return userAccess.role

        val groupAccess = if (groupId != null) {
            groupGameTacticAccessRepository.findByGameTacticIdAndGroupId(gameTacticId, groupId)
                ?.takeIf { it.group?.members?.any { m -> m.id == userId } == true }
        } else {
            groupGameTacticAccessRepository.findByGameTacticId(gameTacticId)
                .firstOrNull { it.group?.members?.any { m -> m.id == userId } == true }
        }
        if (groupAccess != null) return groupAccess.role

        return if (gameTactic.isPublic || gameTactic.isPremade) AccessRole.VIEWER else AccessRole.NONE
    }
}