package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.CollaboratorDTO
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PracticeSharingService(
    private val practiceRepository: PracticeRepository,
    private val userRepository: UserRepository,
    private val groupRepository: UserGroupRepository,
    private val userPracticeAccessRepository: UserPracticeAccessRepository,
    private val groupPracticeAccessRepository: GroupPracticeAccessRepository
) {

    @Transactional(readOnly = true)
    fun getPracticeCollaborators(ownerId: Int, practiceId: Int): List<CollaboratorDTO> {
        // Fast ownership check without loading the entire Practice entity into memory
        if (!practiceRepository.existsByIdAndOwnerId(practiceId, ownerId)) {
            throw UnauthorizedException("Only the owner can view collaborators or practice does not exist")
        }

        val collaborators = mutableListOf<CollaboratorDTO>()

        // Uses JOIN FETCH to get all users in 1 query (fixes N+1)
        val userAccess = userPracticeAccessRepository.findAllWithUserByPracticeId(practiceId)
            .mapNotNull { access ->
                val user = access.user ?: return@mapNotNull null
                CollaboratorDTO(user.id, user.email, CollaboratorType.USER, access.role)
            }

        // Uses JOIN FETCH to get all groups in 1 query (fixes N+1)
        val groupAccess = groupPracticeAccessRepository.findAllWithGroupByPracticeId(practiceId)
            .mapNotNull { access ->
                val group = access.group ?: return@mapNotNull null
                CollaboratorDTO(group.id, group.name, CollaboratorType.GROUP, access.role)
            }

        collaborators.addAll(userAccess)
        collaborators.addAll(groupAccess)
        return collaborators
    }

    @Transactional
    fun sharePracticeWithUser(ownerId: Int, practiceId: Int, targetUserId: Int, role: AccessRole) {
        if (ownerId == targetUserId) throw IllegalArgumentException("Cannot share with yourself")

        if (!practiceRepository.existsByIdAndOwnerId(practiceId, ownerId)) {
            throw UnauthorizedException("You do not have permission to share this practice")
        }

        val existingAccess = userPracticeAccessRepository.findByUserIdAndPracticeId(targetUserId, practiceId)
        if (existingAccess != null) {
            existingAccess.role = role
            // Hibernate auto-updates managed entities on transaction commit; no .save() needed
        } else {
            // getReferenceById creates a Proxy. No SELECT queries are executed!
            val practiceProxy = practiceRepository.getReferenceById(practiceId)
            val userProxy = userRepository.getReferenceById(targetUserId)
            userPracticeAccessRepository.save(UserPracticeAccess(user = userProxy, practice = practiceProxy, role = role))
        }
    }

    @Transactional
    fun revokePracticeFromUser(ownerId: Int, practiceId: Int, targetUserId: Int) {
        if (!practiceRepository.existsByIdAndOwnerId(practiceId, ownerId)) {
            throw UnauthorizedException("You do not have permission to revoke access to this practice")
        }

        // Single DB command: DELETE WHERE userId = ? AND practiceId = ?
        userPracticeAccessRepository.deleteByUserIdAndPracticeId(targetUserId, practiceId)
    }

    @Transactional
    fun sharePracticeWithGroup(ownerId: Int, practiceId: Int, groupId: Int, role: AccessRole) {
        if (!practiceRepository.existsByIdAndOwnerId(practiceId, ownerId)) {
            throw UnauthorizedException("You do not have permission to share this practice")
        }

        val existingAccess = groupPracticeAccessRepository.findByPracticeIdAndGroupId(practiceId, groupId)
        if (existingAccess != null) {
            existingAccess.role = role
        } else {
            // getReferenceById creates a Proxy. No SELECT queries are executed!
            val practiceProxy = practiceRepository.getReferenceById(practiceId)
            val groupProxy = groupRepository.getReferenceById(groupId)
            groupPracticeAccessRepository.save(GroupPracticeAccess(practice = practiceProxy, group = groupProxy, role = role))
        }
    }

    @Transactional
    fun revokePracticeFromGroup(ownerId: Int, practiceId: Int, groupId: Int) {
        if (!practiceRepository.existsByIdAndOwnerId(practiceId, ownerId)) {
            throw UnauthorizedException("You do not have permission to revoke access to this practice")
        }

        // Single DB command: DELETE WHERE groupId = ? AND practiceId = ?
        groupPracticeAccessRepository.deleteByGroupIdAndPracticeId(groupId, practiceId)
    }
}