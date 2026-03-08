package org.sportstechsolutions.apitacticsapp.service

import org.slf4j.LoggerFactory
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

    private val log = LoggerFactory.getLogger(GameTacticSharingService::class.java)

    @Transactional(readOnly = true)
    fun getGameTacticCollaborators(ownerId: Int, tacticId: Int): List<CollaboratorDTO> {
        log.debug("Fetching collaborators for Game Tactic ID: $tacticId requested by User ID: $ownerId")

        // Fast ownership check without loading the entire GameTactic entity into memory
        if (!gameTacticRepository.existsByIdAndOwnerId(tacticId, ownerId)) {
            log.warn("Access denied: User ID $ownerId is not the owner of Game Tactic ID $tacticId")
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

        log.debug("Successfully retrieved ${collaborators.size} collaborators for Game Tactic ID: $tacticId")
        return collaborators
    }

    @Transactional
    fun shareGameTacticWithUser(ownerId: Int, tacticId: Int, targetUserId: Int, role: AccessRole) {
        log.info("Attempting to share Game Tactic ID: $tacticId with User ID: $targetUserId by Owner ID: $ownerId")

        if (ownerId == targetUserId) {
            log.warn("Share rejected: User ID $ownerId attempted to share Game Tactic ID $tacticId with themselves")
            throw IllegalArgumentException("Cannot share with yourself")
        }

        if (!gameTacticRepository.existsByIdAndOwnerId(tacticId, ownerId)) {
            log.warn("Share rejected: User ID $ownerId is not the owner of Game Tactic ID $tacticId")
            throw UnauthorizedException("You do not have permission to share this tactic")
        }

        val existingAccess = userGameTacticAccessRepository.findByUserIdAndGameTacticId(targetUserId, tacticId)
        if (existingAccess != null) {
            log.debug("Updating existing access role for User ID $targetUserId on Game Tactic ID $tacticId to $role")
            existingAccess.role = role
            // Hibernate auto-updates managed entities on transaction commit; no .save() needed
        } else {
            log.debug("Creating new access grant for User ID $targetUserId on Game Tactic ID $tacticId with role $role")
            // getReferenceById creates a Proxy. No SELECT queries are executed!
            val tacticProxy = gameTacticRepository.getReferenceById(tacticId)
            val userProxy = userRepository.getReferenceById(targetUserId)
            userGameTacticAccessRepository.save(UserGameTacticAccess(user = userProxy, gameTactic = tacticProxy, role = role))
        }
        log.info("Successfully shared Game Tactic ID: $tacticId with User ID: $targetUserId")
    }

    @Transactional
    fun revokeGameTacticFromUser(ownerId: Int, tacticId: Int, targetUserId: Int) {
        log.info("Attempting to revoke access to Game Tactic ID: $tacticId from User ID: $targetUserId by Owner ID: $ownerId")

        if (!gameTacticRepository.existsByIdAndOwnerId(tacticId, ownerId)) {
            log.warn("Revoke rejected: User ID $ownerId is not the owner of Game Tactic ID $tacticId")
            throw UnauthorizedException("You do not have permission to revoke access to this tactic")
        }

        // Single DB command: DELETE WHERE userId = ? AND tacticId = ?
        userGameTacticAccessRepository.deleteByUserIdAndGameTacticId(targetUserId, tacticId)
        log.info("Successfully revoked access to Game Tactic ID: $tacticId for User ID: $targetUserId")
    }

    @Transactional
    fun shareGameTacticWithGroup(ownerId: Int, tacticId: Int, groupId: Int, role: AccessRole) {
        log.info("Attempting to share Game Tactic ID: $tacticId with Group ID: $groupId by Owner ID: $ownerId")

        if (!gameTacticRepository.existsByIdAndOwnerId(tacticId, ownerId)) {
            log.warn("Share rejected: User ID $ownerId is not the owner of Game Tactic ID $tacticId")
            throw UnauthorizedException("You do not have permission to share this tactic")
        }

        val existingAccess = groupGameTacticAccessRepository.findByGameTacticIdAndGroupId(tacticId, groupId)
        if (existingAccess != null) {
            log.debug("Updating existing access role for Group ID $groupId on Game Tactic ID $tacticId to $role")
            existingAccess.role = role
        } else {
            log.debug("Creating new access grant for Group ID $groupId on Game Tactic ID $tacticId with role $role")
            // getReferenceById creates a Proxy. No SELECT queries are executed!
            val tacticProxy = gameTacticRepository.getReferenceById(tacticId)
            val groupProxy = groupRepository.getReferenceById(groupId)
            groupGameTacticAccessRepository.save(GroupGameTacticAccess(gameTactic = tacticProxy, group = groupProxy, role = role))
        }
        log.info("Successfully shared Game Tactic ID: $tacticId with Group ID: $groupId")
    }

    @Transactional
    fun revokeGameTacticFromGroup(ownerId: Int, tacticId: Int, groupId: Int) {
        log.info("Attempting to revoke access to Game Tactic ID: $tacticId from Group ID: $groupId by Owner ID: $ownerId")

        if (!gameTacticRepository.existsByIdAndOwnerId(tacticId, ownerId)) {
            log.warn("Revoke rejected: User ID $ownerId is not the owner of Game Tactic ID $tacticId")
            throw UnauthorizedException("You do not have permission to revoke access to this tactic")
        }

        // Single DB command: DELETE WHERE groupId = ? AND tacticId = ?
        groupGameTacticAccessRepository.deleteByGroupIdAndGameTacticId(groupId, tacticId)
        log.info("Successfully revoked access to Game Tactic ID: $tacticId for Group ID: $groupId")
    }
}