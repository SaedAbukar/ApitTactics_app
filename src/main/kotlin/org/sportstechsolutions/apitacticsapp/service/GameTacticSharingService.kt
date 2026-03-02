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
        val tactic = gameTacticRepository.findById(tacticId).orElseThrow { ResourceNotFoundException("Tactic not found") }
        if (tactic.owner?.id != ownerId) throw UnauthorizedException("Access denied")

        val collaborators = mutableListOf<CollaboratorDTO>()

        val userAccess = userGameTacticAccessRepository.findByGameTacticId(tacticId)
            .mapNotNull { access ->
                access.user?.let { CollaboratorDTO(it.id, it.email, CollaboratorType.USER, access.role) }
            }

        val groupAccess = groupGameTacticAccessRepository.findByGameTacticId(tacticId)
            .mapNotNull { access ->
                access.group?.let { CollaboratorDTO(it.id, it.name, CollaboratorType.GROUP, access.role) }
            }

        collaborators.addAll(userAccess)
        collaborators.addAll(groupAccess)
        return collaborators
    }

    @Transactional
    fun shareGameTacticWithUser(ownerId: Int, tacticId: Int, targetUserId: Int, role: AccessRole) {
        if (ownerId == targetUserId) throw IllegalArgumentException("Cannot share with yourself")

        val tactic = gameTacticRepository.findById(tacticId).orElseThrow { ResourceNotFoundException("Tactic not found") }
        if (tactic.owner?.id != ownerId) throw UnauthorizedException("Not the owner")

        val user = userRepository.findById(targetUserId).orElseThrow { ResourceNotFoundException("User not found") }

        val existingAccess = userGameTacticAccessRepository.findByUserIdAndGameTacticId(targetUserId, tacticId)
        if (existingAccess != null) {
            existingAccess.role = role
            userGameTacticAccessRepository.save(existingAccess)
        } else {
            userGameTacticAccessRepository.save(UserGameTacticAccess(user = user, gameTactic = tactic, role = role))
        }
    }

    @Transactional
    fun revokeGameTacticFromUser(ownerId: Int, tacticId: Int, targetUserId: Int) {
        val tactic = gameTacticRepository.findById(tacticId).orElseThrow { ResourceNotFoundException("Tactic not found") }
        if (tactic.owner?.id != ownerId) throw UnauthorizedException("Not the owner")

        val access = userGameTacticAccessRepository.findByUserIdAndGameTacticId(targetUserId, tacticId)
            ?: throw ResourceNotFoundException("Access not found")

        userGameTacticAccessRepository.delete(access)
    }

    @Transactional
    fun shareGameTacticWithGroup(ownerId: Int, tacticId: Int, groupId: Int, role: AccessRole) {
        val tactic = gameTacticRepository.findById(tacticId).orElseThrow { ResourceNotFoundException("Tactic not found") }
        if (tactic.owner?.id != ownerId) throw UnauthorizedException("Not the owner")

        val group = groupRepository.findById(groupId).orElseThrow { ResourceNotFoundException("Group not found") }

        val existingAccess = groupGameTacticAccessRepository.findByGameTacticIdAndGroupId(tacticId, groupId)
        if (existingAccess != null) {
            existingAccess.role = role
            groupGameTacticAccessRepository.save(existingAccess)
        } else {
            groupGameTacticAccessRepository.save(GroupGameTacticAccess(gameTactic = tactic, group = group, role = role))
        }
    }

    @Transactional
    fun revokeGameTacticFromGroup(ownerId: Int, tacticId: Int, groupId: Int) {
        val tactic = gameTacticRepository.findById(tacticId).orElseThrow { ResourceNotFoundException("Tactic not found") }
        if (tactic.owner?.id != ownerId) throw UnauthorizedException("Not the owner")

        val access = groupGameTacticAccessRepository.findByGameTacticIdAndGroupId(tacticId, groupId)
            ?: throw ResourceNotFoundException("Access not found")

        groupGameTacticAccessRepository.delete(access)
    }
}