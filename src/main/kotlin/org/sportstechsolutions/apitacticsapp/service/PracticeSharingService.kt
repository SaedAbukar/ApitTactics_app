package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.CollaboratorDTO
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
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
        val practice = practiceRepository.findById(practiceId).orElseThrow { ResourceNotFoundException("Practice not found") }
        if (practice.owner?.id != ownerId) throw UnauthorizedException("Access denied")

        val collaborators = mutableListOf<CollaboratorDTO>()

        val userAccess = userPracticeAccessRepository.findByPracticeId(practiceId)
            .mapNotNull { access ->
                access.user?.let { CollaboratorDTO(it.id, it.email, CollaboratorType.USER, access.role) }
            }

        val groupAccess = groupPracticeAccessRepository.findByPracticeId(practiceId)
            .mapNotNull { access ->
                access.group?.let { CollaboratorDTO(it.id, it.name, CollaboratorType.GROUP, access.role) }
            }

        collaborators.addAll(userAccess)
        collaborators.addAll(groupAccess)
        return collaborators
    }

    @Transactional
    fun sharePracticeWithUser(ownerId: Int, practiceId: Int, targetUserId: Int, role: AccessRole) {
        if (ownerId == targetUserId) throw IllegalArgumentException("Cannot share with yourself")

        val practice = practiceRepository.findById(practiceId).orElseThrow { ResourceNotFoundException("Practice not found") }
        if (practice.owner?.id != ownerId) throw UnauthorizedException("Not the owner")

        val user = userRepository.findById(targetUserId).orElseThrow { ResourceNotFoundException("User not found") }

        val existingAccess = userPracticeAccessRepository.findByUserIdAndPracticeId(targetUserId, practiceId)
        if (existingAccess != null) {
            existingAccess.role = role
            userPracticeAccessRepository.save(existingAccess)
        } else {
            userPracticeAccessRepository.save(UserPracticeAccess(user = user, practice = practice, role = role))
        }
    }

    @Transactional
    fun revokePracticeFromUser(ownerId: Int, practiceId: Int, targetUserId: Int) {
        val practice = practiceRepository.findById(practiceId).orElseThrow { ResourceNotFoundException("Practice not found") }
        if (practice.owner?.id != ownerId) throw UnauthorizedException("Not the owner")

        val access = userPracticeAccessRepository.findByUserIdAndPracticeId(targetUserId, practiceId)
            ?: throw ResourceNotFoundException("Access not found")

        userPracticeAccessRepository.delete(access)
    }

    @Transactional
    fun sharePracticeWithGroup(ownerId: Int, practiceId: Int, groupId: Int, role: AccessRole) {
        val practice = practiceRepository.findById(practiceId).orElseThrow { ResourceNotFoundException("Practice not found") }
        if (practice.owner?.id != ownerId) throw UnauthorizedException("Not the owner")

        val group = groupRepository.findById(groupId).orElseThrow { ResourceNotFoundException("Group not found") }

        val existingAccess = groupPracticeAccessRepository.findByPracticeIdAndGroupId(practiceId, groupId)
        if (existingAccess != null) {
            existingAccess.role = role
            groupPracticeAccessRepository.save(existingAccess)
        } else {
            groupPracticeAccessRepository.save(GroupPracticeAccess(practice = practice, group = group, role = role))
        }
    }

    @Transactional
    fun revokePracticeFromGroup(ownerId: Int, practiceId: Int, groupId: Int) {
        val practice = practiceRepository.findById(practiceId).orElseThrow { ResourceNotFoundException("Practice not found") }
        if (practice.owner?.id != ownerId) throw UnauthorizedException("Not the owner")

        val access = groupPracticeAccessRepository.findByPracticeIdAndGroupId(practiceId, groupId)
            ?: throw ResourceNotFoundException("Access not found")

        groupPracticeAccessRepository.delete(access)
    }
}