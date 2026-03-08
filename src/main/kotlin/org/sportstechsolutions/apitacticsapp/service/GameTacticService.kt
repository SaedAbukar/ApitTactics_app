package org.sportstechsolutions.apitacticsapp.service

import org.slf4j.LoggerFactory
import org.sportstechsolutions.apitacticsapp.dtos.*
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
class GameTacticService(
    private val gameTacticRepository: GameTacticRepository,
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val userGameTacticAccessRepository: UserGameTacticAccessRepository,
    private val groupGameTacticAccessRepository: GroupGameTacticAccessRepository,
    private val entityMappers: EntityMappers,
) {

    private val log = LoggerFactory.getLogger(GameTacticService::class.java)

    @Transactional(readOnly = true)
    fun getGameTacticsForTabs(userId: Int, pageable: Pageable): TabbedResponse<GameTacticSummaryResponse> {
        log.debug("Fetching tabbed game tactics for User ID: $userId")

        val personalPage = gameTacticRepository.findByOwnerId(userId, pageable)
        val personalPaged = PagedResponse(
            content = personalPage.content.map { entityMappers.toGameTacticSummary(it, AccessRole.OWNER, userId) },
            pageNumber = personalPage.number,
            pageSize = personalPage.size,
            totalElements = personalPage.totalElements,
            totalPages = personalPage.totalPages,
            isLast = personalPage.isLast
        )

        // DB level filter prevents pagination count mismatch
        val userSharedPage = userGameTacticAccessRepository.findByUserIdAndRoleNot(userId, AccessRole.NONE, pageable)
        val userSharedPaged = PagedResponse(
            content = userSharedPage.content.mapNotNull { access ->
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

        log.debug("Successfully loaded tabbed game tactics for User ID: $userId")
        return TabbedResponse(personalPaged, userSharedPaged, groupSharedPaged)
    }

    @Transactional(readOnly = true)
    fun searchGameTactics(userId: Int, request: GameTacticSearchRequest, pageable: Pageable): PagedResponse<GameTacticSummaryResponse> {
        log.debug("Executing game tactic search for User ID: $userId with SortBy: ${request.sortBy}")

        // Build Spec using DB-level EXISTS subqueries, removing the in-memory array
        val spec = SearchSpecifications.buildGameTacticSearchSpec(request, userId)

        val finalPageable = if (request.sortBy == SortBy.VIEWS) {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "viewCount"))
        } else {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "id"))
        }

        val tacticPage = gameTacticRepository.findAll(spec, finalPageable)
        val tacticIds = tacticPage.content.mapNotNull { it.id }

        // Bulk fetch roles to prevent N+1 queries during DTO mapping
        val userRoleMap = if (userId != 0 && tacticIds.isNotEmpty()) {
            userGameTacticAccessRepository.findRolesForUserInGameTactics(userId, tacticIds).associate { it.gameTacticId to it.role }
        } else emptyMap()

        val groupRoleMap = if (userId != 0 && tacticIds.isNotEmpty()) {
            groupGameTacticAccessRepository.findRolesForUserInGameTactics(userId, tacticIds).associate { it.gameTacticId to it.role }
        } else emptyMap()

        val content = tacticPage.content.map { tactic ->
            val tacticId = tactic.id ?: 0

            val role = when {
                userId == 0 -> AccessRole.VIEWER
                tactic.owner?.id == userId -> AccessRole.OWNER
                userRoleMap.containsKey(tacticId) -> userRoleMap[tacticId]!!
                groupRoleMap.containsKey(tacticId) -> groupRoleMap[tacticId]!!
                tactic.isPublic || tactic.isPremade -> AccessRole.VIEWER
                else -> AccessRole.NONE
            }
            entityMappers.toGameTacticSummary(tactic, role, userId)
        }

        log.debug("Game tactic search completed. Found ${tacticPage.totalElements} total elements.")
        return PagedResponse(
            content = content,
            pageNumber = tacticPage.number,
            pageSize = tacticPage.size,
            totalElements = tacticPage.totalElements,
            totalPages = tacticPage.totalPages,
            isLast = tacticPage.isLast
        )
    }

    @Transactional
    fun createGameTactic(userId: Int, request: GameTacticRequest): GameTacticResponse {
        log.info("Attempting to create a new game tactic for User ID: $userId")

        val user = userRepository.findById(userId).orElseThrow {
            log.error("Create game tactic failed: User ID $userId not found in database.")
            ResourceNotFoundException("User not found")
        }

        val gameTactic = GameTactic(
            name = request.name,
            description = request.description,
            isPremade = request.isPremade,
            isPublic = request.isPublic,
            owner = user
        )

        val sessionsToAttach = request.sessions.map { dto ->
            val sessionId = dto.id ?: throw IllegalArgumentException("Session ID required")
            sessionRepository.findById(sessionId).orElseThrow {
                log.warn("Create game tactic failed: Child Session ID $sessionId not found.")
                ResourceNotFoundException("Session $sessionId not found")
            }
        }

        sessionsToAttach.forEach { session -> session.gameTactics.add(gameTactic) }
        gameTactic.sessions.addAll(sessionsToAttach)

        val saved = gameTacticRepository.save(gameTactic)
        log.info("Successfully created Game Tactic ID: ${saved.id} for User ID: $userId")
        return entityMappers.loadFullGameTactic(saved, AccessRole.OWNER, userId)
    }

    @Transactional
    fun updateGameTactic(userId: Int, gameTacticId: Int, request: GameTacticRequest, groupId: Int? = null): GameTacticResponse {
        log.info("Attempting to update Game Tactic ID: $gameTacticId for User ID: $userId")

        val gameTactic = gameTacticRepository.findById(gameTacticId).orElseThrow {
            log.warn("Update failed: Game Tactic ID $gameTacticId not found.")
            ResourceNotFoundException("Game tactic not found")
        }

        // Prevent redundant database fetch
        val role = getUserRoleForGameTactic(userId, gameTactic, groupId)

        if (!role.canEdit()) {
            log.warn("Update rejected: User ID $userId lacks edit permission (Role: $role) for Game Tactic ID: $gameTacticId")
            throw UnauthorizedException("You do not have permission to edit this game tactic")
        }

        gameTactic.name = request.name
        gameTactic.description = request.description
        gameTactic.isPremade = request.isPremade
        gameTactic.isPublic = request.isPublic

        gameTactic.sessions.forEach { session -> session.gameTactics.remove(gameTactic) }
        gameTactic.sessions.clear()

        val sessionsToAttach = request.sessions.map { dto ->
            val sessionId = dto.id ?: throw IllegalArgumentException("Session ID required")
            sessionRepository.findById(sessionId).orElseThrow {
                log.warn("Update game tactic failed: Child Session ID $sessionId not found.")
                ResourceNotFoundException("Session $sessionId not found")
            }
        }

        sessionsToAttach.forEach { session -> session.gameTactics.add(gameTactic) }
        gameTactic.sessions.addAll(sessionsToAttach)

        val updated = gameTacticRepository.save(gameTactic)
        log.info("Successfully updated Game Tactic ID: $gameTacticId")
        return entityMappers.loadFullGameTactic(updated, role, userId)
    }

    @Transactional
    fun deleteGameTactic(userId: Int, gameTacticId: Int, groupId: Int? = null) {
        log.info("Attempting to delete Game Tactic ID: $gameTacticId for User ID: $userId")

        val gameTactic = gameTacticRepository.findById(gameTacticId).orElseThrow {
            log.warn("Delete failed: Game Tactic ID $gameTacticId not found.")
            ResourceNotFoundException("Game tactic not found")
        }

        val role = getUserRoleForGameTactic(userId, gameTactic, groupId)

        if (!role.canEdit()) {
            log.warn("Delete rejected: User ID $userId lacks edit permission (Role: $role) for Game Tactic ID: $gameTacticId")
            throw UnauthorizedException("You do not have permission to delete this game tactic")
        }

        gameTactic.sessions.forEach { it.gameTactics.remove(gameTactic) }

        userGameTacticAccessRepository.deleteAllByGameTactic(gameTactic)
        gameTacticRepository.delete(gameTactic)
        log.info("Successfully deleted Game Tactic ID: $gameTacticId")
    }

    @Transactional
    fun getGameTacticById(gameTacticId: Int, userId: Int, groupId: Int? = null): GameTacticResponse {
        log.debug("Fetching Game Tactic ID: $gameTacticId for User ID: $userId")

        val gameTactic = gameTacticRepository.findById(gameTacticId).orElseThrow {
            log.warn("Fetch failed: Game Tactic ID $gameTacticId not found.")
            ResourceNotFoundException("Game tactic not found")
        }

        val role = getUserRoleForGameTactic(userId, gameTactic, groupId)

        if (role == AccessRole.NONE) {
            log.warn("Access denied: User ID $userId attempted to view private Game Tactic ID: $gameTacticId")
            throw UnauthorizedException("You do not have access to this game tactic")
        }

        gameTactic.viewCount += 1
        gameTacticRepository.save(gameTactic)

        log.debug("Successfully retrieved Game Tactic ID: $gameTacticId with Role: $role")
        return entityMappers.loadFullGameTactic(gameTactic, role, userId)
    }

    @Transactional
    fun toggleFavorite(userId: Int, gameTacticId: Int): Boolean {
        log.info("Attempting to toggle favorite for Game Tactic ID: $gameTacticId by User ID: $userId")

        val role = getUserRoleForGameTactic(userId, gameTacticId)
        if (role == AccessRole.NONE) {
            log.warn("Favorite toggle rejected: User ID $userId does not have access to Game Tactic ID: $gameTacticId")
            throw UnauthorizedException("You do not have access to this game tactic")
        }

        val isAlreadyFavorite = gameTacticRepository.isGameTacticFavoritedByUser(gameTacticId, userId)
        if (isAlreadyFavorite) {
            gameTacticRepository.removeFavorite(gameTacticId, userId)
            log.info("Successfully removed favorite for Game Tactic ID: $gameTacticId by User ID: $userId")
        } else {
            gameTacticRepository.addFavorite(gameTacticId, userId)
            log.info("Successfully added favorite for Game Tactic ID: $gameTacticId by User ID: $userId")
        }
        return !isAlreadyFavorite
    }

    // Main resolver: operates on the entity to prevent double fetching
    fun getUserRoleForGameTactic(userId: Int, gameTactic: GameTactic, groupId: Int? = null): AccessRole {
        val tacticId = gameTactic.id ?: return AccessRole.NONE
        if (gameTactic.owner?.id == userId) return AccessRole.OWNER

        val userAccess = userGameTacticAccessRepository.findByUserIdAndGameTacticId(userId, tacticId)
        if (userAccess != null) return userAccess.role

        val groupRole = groupGameTacticAccessRepository.findRoleForUserInGameTactic(userId, tacticId)
        if (groupRole != null) return groupRole

        return if (gameTactic.isPublic || gameTactic.isPremade) AccessRole.VIEWER else AccessRole.NONE
    }

    // Fallback resolver
    fun getUserRoleForGameTactic(userId: Int, gameTacticId: Int, groupId: Int? = null): AccessRole {
        val gameTactic = gameTacticRepository.findById(gameTacticId).orElseThrow { ResourceNotFoundException("Game tactic not found") }
        return getUserRoleForGameTactic(userId, gameTactic, groupId)
    }
}