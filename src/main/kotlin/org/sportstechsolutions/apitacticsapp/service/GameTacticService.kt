package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GameTacticService(
    private val gameTacticRepository: GameTacticRepository,
    private val userGameTacticAccessRepository: UserGameTacticAccessRepository,
    private val groupGameTacticAccessRepository: GroupGameTacticAccessRepository,
    private val entityMappers: EntityMappers
) {

    // ------------------------------------------
    // Tabbed view (personal, user shared, group shared)
    // ------------------------------------------
    @Transactional(readOnly = true)
    fun getGameTacticsForTabs(userId: Int): TabbedResponse<GameTacticResponse> {
        val personal = gameTacticRepository.findByOwnerId(userId)
            .map { entityMappers.loadFullGameTactic(it) }

        val userShared = userGameTacticAccessRepository.findByUserId(userId)
            .filter { it.role != AccessRole.NONE }
            .mapNotNull { it.gameTactic }
            .map { entityMappers.loadFullGameTactic(it) }

        val groupShared = groupGameTacticAccessRepository.findByGroupMemberId(userId)
            .mapNotNull { it.gameTactic }
            .distinct()
            .map { entityMappers.loadFullGameTactic(it) }

        return TabbedResponse(
            personalItems = personal,
            userSharedItems = userShared,
            groupSharedItems = groupShared
        )
    }

    // ------------------------------------------
    // CRUD
    // ------------------------------------------
    @Transactional
    fun createGameTactic(userId: Int, request: GameTacticRequest): GameTacticResponse {
        val gameTactic = GameTactic(
            name = request.name,
            description = request.description,
            is_premade = request.isPremade,
            owner = User(id = userId)
        )

        val sessions = request.sessions.map { entityMappers.toSession(it, gameTactic) }
        sessions.forEach { it.gameTactics.add(gameTactic) }
        gameTactic.sessions.addAll(sessions)

        val saved = gameTacticRepository.save(gameTactic)
        return entityMappers.loadFullGameTactic(saved)
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
        gameTactic.is_premade = request.isPremade
        gameTactic.sessions.forEach { it.gameTactics.remove(gameTactic) }
        gameTactic.sessions.clear()

        val newSessions = request.sessions.map { entityMappers.toSession(it, gameTactic) }
        newSessions.forEach { it.gameTactics.add(gameTactic) }
        gameTactic.sessions.addAll(newSessions)

        val updated = gameTacticRepository.save(gameTactic)
        return entityMappers.loadFullGameTactic(updated)
    }

    @Transactional
    fun deleteGameTactic(userId: Int, gameTacticId: Int, groupId: Int? = null) {
        val gameTactic = gameTacticRepository.findById(gameTacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        val role = if (gameTactic.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForGameTactic(userId, gameTacticId, groupId)

        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to delete this game tactic")

        gameTacticRepository.delete(gameTactic)
    }

    @Transactional(readOnly = true)
    fun getGameTacticById(gameTacticId: Int, userId: Int, groupId: Int? = null): GameTacticResponse {
        val gameTactic = gameTacticRepository.findById(gameTacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        val role = if (gameTactic.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForGameTactic(userId, gameTacticId, groupId)

        if (role == AccessRole.NONE) throw UnauthorizedException("You do not have access to this game tactic")
        return entityMappers.loadFullGameTactic(gameTactic)
    }

    // ------------------------------------------
    // Access role resolver
    // ------------------------------------------
    fun getUserRoleForGameTactic(userId: Int, gameTacticId: Int, groupId: Int? = null): AccessRole {
        val gameTactic = gameTacticRepository.findById(gameTacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        if (gameTactic.owner?.id == userId) return AccessRole.OWNER

        val userAccess = userGameTacticAccessRepository.findByUserIdAndGameTacticId(userId, gameTacticId)
        if (userAccess != null) return userAccess.role

        val groupAccess = if (groupId != null) {
            groupGameTacticAccessRepository.findByGameTacticIdAndGroupId(gameTacticId, groupId)
                ?.takeIf { it.group?.members?.any { m -> m.id == userId } == true }
        } else {
            groupGameTacticAccessRepository.findByGameTacticId(gameTacticId)
                ?.firstOrNull { it.group?.members?.any { m -> m.id == userId } == true }
        }

        return groupAccess?.role ?: AccessRole.NONE
    }
}
