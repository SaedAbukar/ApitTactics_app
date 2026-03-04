package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.CollaboratorDTO
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GameTacticSharingService(
    private val gameTacticRepository: GameTacticRepository,
    private val userRepository: UserRepository,
    private val groupRepository: UserGroupRepository,
    private val userGameTacticAccessRepository: UserGameTacticAccessRepository,
    private val groupGameTacticAccessRepository: GroupGameTacticAccessRepository
) {

    @Transactional(readOnly = true)
    fun getGameTacticCollaborators(ownerId: Int, tacticId: Int): List<CollaboratorDTO> {
        // Fast ownership check without loading the entire GameTactic entity into memory
        if (!gameTacticRepository.existsByIdAndOwnerId(tacticId, ownerId)) {
            throw UnauthorizedException("Only the owner can view collaborators or tactic does not exist")
        }

        val collaborators = mutableListOf<CollaboratorDTO>()

        // Uses JOIN FETCH to get all users in 1 query (fixes N+1)
        val userAccess = userGameTacticAccessRepository.findAllWithUserByGameTacticId(tacticId)
            .mapNotNull { access ->
                val user = access.user ?: return@mapNotNull null
                CollaboratorDTO(user.id, user.email, CollaboratorType.USER, access.role)
            }

        // Uses JOIN FETCH to get all groups in 1 query (fixes N+1)
        val groupAccess = groupGameTacticAccessRepository.findAllWithGroupByGameTacticId(tacticId)
            .mapNotNull { access ->
                val group = access.group ?: return@mapNotNull null
                CollaboratorDTO(group.id, group.name, CollaboratorType.GROUP, access.role)
            }

        collaborators.addAll(userAccess)
        collaborators.addAll(groupAccess)
        return collaborators
    }

    @Transactional
    fun shareGameTacticWithUser(ownerId: Int, tacticId: Int, targetUserId: Int, role: AccessRole) {
        if (ownerId == targetUserId) throw IllegalArgumentException("Cannot share with yourself")

        if (!gameTacticRepository.existsByIdAndOwnerId(tacticId, ownerId)) {
            throw UnauthorizedException("You do not have permission to share this tactic")
        }

        val existingAccess = userGameTacticAccessRepository.findByUserIdAndGameTacticId(targetUserId, tacticId)
        if (existingAccess != null) {
            existingAccess.role = role
            // Hibernate auto-updates managed entities on transaction commit; no .save() needed
        } else {
            // getReferenceById creates a Proxy. No SELECT queries are executed!
            val tacticProxy = gameTacticRepository.getReferenceById(tacticId)
            val userProxy = userRepository.getReferenceById(targetUserId)
            userGameTacticAccessRepository.save(UserGameTacticAccess(user = userProxy, gameTactic = tacticProxy, role = role))
        }
    }

    @Transactional
    fun revokeGameTacticFromUser(ownerId: Int, tacticId: Int, targetUserId: Int) {
        if (!gameTacticRepository.existsByIdAndOwnerId(tacticId, ownerId)) {
            throw UnauthorizedException("You do not have permission to revoke access to this tactic")
        }

        // Single DB command: DELETE WHERE userId = ? AND tacticId = ?
        userGameTacticAccessRepository.deleteByUserIdAndGameTacticId(targetUserId, tacticId)
    }

    @Transactional
    fun shareGameTacticWithGroup(ownerId: Int, tacticId: Int, groupId: Int, role: AccessRole) {
        if (!gameTacticRepository.existsByIdAndOwnerId(tacticId, ownerId)) {
            throw UnauthorizedException("You do not have permission to share this tactic")
        }

        val existingAccess = groupGameTacticAccessRepository.findByGameTacticIdAndGroupId(tacticId, groupId)
        if (existingAccess != null) {
            existingAccess.role = role
        } else {
            // getReferenceById creates a Proxy. No SELECT queries are executed!
            val tacticProxy = gameTacticRepository.getReferenceById(tacticId)
            val groupProxy = groupRepository.getReferenceById(groupId)
            groupGameTacticAccessRepository.save(GroupGameTacticAccess(gameTactic = tacticProxy, group = groupProxy, role = role))
        }
    }

    @Transactional
    fun revokeGameTacticFromGroup(ownerId: Int, tacticId: Int, groupId: Int) {
        if (!gameTacticRepository.existsByIdAndOwnerId(tacticId, ownerId)) {
            throw UnauthorizedException("You do not have permission to revoke access to this tactic")
        }

        // Single DB command: DELETE WHERE groupId = ? AND tacticId = ?
        groupGameTacticAccessRepository.deleteByGroupIdAndGameTacticId(groupId, tacticId)
    }
}