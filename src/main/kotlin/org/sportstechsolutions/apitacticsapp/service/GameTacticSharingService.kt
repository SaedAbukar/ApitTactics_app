package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.CollaboratorDTO
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
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
        val tactic = gameTacticRepository.findById(tacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        val owner = tactic.owner
            ?: throw IllegalStateException("Game tactic has no owner")

        // Security check: only owner can view collaborators
        if (owner.id != ownerId) {
            throw UnauthorizedException("Access denied")
        }

        val collaborators = mutableListOf<CollaboratorDTO>()

        // --- USER COLLABORATORS ---
        val userAccess = userGameTacticAccessRepository.findByGameTacticId(tacticId)
            .mapNotNull { access ->
                val user = access.user ?: return@mapNotNull null

                // Skip owner if somehow present
                if (user.id == owner.id) return@mapNotNull null

                CollaboratorDTO(
                    id = user.id,
                    name = user.email,
                    type = CollaboratorType.USER,
                    role = access.role
                )
            }

        // --- GROUP COLLABORATORS ---
        val groupAccess = groupGameTacticAccessRepository.findByGameTacticId(tacticId)
            .mapNotNull { access ->
                val group = access.group ?: return@mapNotNull null

                CollaboratorDTO(
                    id = group.id,
                    name = group.name,
                    type = CollaboratorType.GROUP,
                    role = access.role
                )
            }

        collaborators.addAll(userAccess)
        collaborators.addAll(groupAccess)

        return collaborators
    }

    // -----------------------------
    // User sharing
    // -----------------------------
    @Transactional
    fun shareGameTacticWithUser(ownerId: Int, tacticId: Int, targetUserId: Int, role: AccessRole) {
        val tactic = gameTacticRepository.findById(tacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        if (tactic.owner?.id != ownerId) throw UnauthorizedException("Only the owner can share this tactic")

        val user = userRepository.findById(targetUserId)
            .orElseThrow { ResourceNotFoundException("User not found") }

        val existingAccess = userGameTacticAccessRepository.findByUserIdAndGameTacticId(targetUserId, tacticId)
        if (existingAccess != null) {
            existingAccess.role = role
            userGameTacticAccessRepository.save(existingAccess)
        } else {
            val access = UserGameTacticAccess(user = user, gameTactic = tactic, role = role)
            userGameTacticAccessRepository.save(access)
        }
    }

    @Transactional
    fun revokeGameTacticFromUser(ownerId: Int, tacticId: Int, targetUserId: Int) {
        val tactic = gameTacticRepository.findById(tacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        if (tactic.owner?.id != ownerId) throw UnauthorizedException("Only the owner can revoke access")

        val access = userGameTacticAccessRepository.findByUserIdAndGameTacticId(targetUserId, tacticId)
            ?: throw ResourceNotFoundException("No shared access found for this user and tactic")

        userGameTacticAccessRepository.delete(access)
    }

    // -----------------------------
    // Group sharing
    // -----------------------------
    @Transactional
    fun shareGameTacticWithGroup(ownerId: Int, tacticId: Int, groupId: Int, role: AccessRole) {
        val tactic = gameTacticRepository.findById(tacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        if (tactic.owner?.id != ownerId) throw UnauthorizedException("Only the owner can share this tactic")

        val group = groupRepository.findById(groupId)
            .orElseThrow { ResourceNotFoundException("Group not found") }

        val existingAccess = groupGameTacticAccessRepository.findByGameTacticIdAndGroupId(tacticId, groupId)
        if (existingAccess != null) {
            existingAccess.role = role
            groupGameTacticAccessRepository.save(existingAccess)
        } else {
            val access = GroupGameTacticAccess(gameTactic = tactic, group = group, role = role)
            groupGameTacticAccessRepository.save(access)
        }
    }

    @Transactional
    fun revokeGameTacticFromGroup(ownerId: Int, tacticId: Int, groupId: Int) {
        val tactic = gameTacticRepository.findById(tacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        if (tactic.owner?.id != ownerId) throw UnauthorizedException("Only the owner can revoke access")

        val access = groupGameTacticAccessRepository.findByGameTacticIdAndGroupId(tacticId, groupId)
            ?: throw ResourceNotFoundException("No shared access found for this group and tactic")

        groupGameTacticAccessRepository.delete(access)
    }
}
