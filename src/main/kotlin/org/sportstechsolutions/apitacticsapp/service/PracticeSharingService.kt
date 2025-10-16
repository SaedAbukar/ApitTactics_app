package org.sportstechsolutions.apitacticsapp.service

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

    // -----------------------------
    // User sharing
    // -----------------------------
    @Transactional
    fun sharePracticeWithUser(ownerId: Int, practiceId: Int, targetUserId: Int, role: AccessRole) {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        if (practice.owner?.id != ownerId) throw UnauthorizedException("Only the owner can share this practice")

        val user = userRepository.findById(targetUserId)
            .orElseThrow { ResourceNotFoundException("User not found") }

        val existingAccess = userPracticeAccessRepository.findByUserIdAndPracticeId(targetUserId, practiceId)
        if (existingAccess != null) {
            existingAccess.role = role
            userPracticeAccessRepository.save(existingAccess)
        } else {
            val access = UserPracticeAccess(user = user, practice = practice, role = role)
            userPracticeAccessRepository.save(access)
        }
    }

    @Transactional
    fun revokePracticeFromUser(ownerId: Int, practiceId: Int, targetUserId: Int) {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        if (practice.owner?.id != ownerId) throw UnauthorizedException("Only the owner can revoke access")

        val access = userPracticeAccessRepository.findByUserIdAndPracticeId(targetUserId, practiceId)
            ?: throw ResourceNotFoundException("No shared access found for this user and practice")

        userPracticeAccessRepository.delete(access)
    }

    // -----------------------------
    // Group sharing
    // -----------------------------
    @Transactional
    fun sharePracticeWithGroup(ownerId: Int, practiceId: Int, groupId: Int, role: AccessRole) {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        if (practice.owner?.id != ownerId) throw UnauthorizedException("Only the owner can share this practice")

        val group = groupRepository.findById(groupId)
            .orElseThrow { ResourceNotFoundException("Group not found") }

        val existingAccess = groupPracticeAccessRepository.findByPracticeIdAndGroupId(practiceId, groupId)
        if (existingAccess != null) {
            existingAccess.role = role
            groupPracticeAccessRepository.save(existingAccess)
        } else {
            val access = GroupPracticeAccess(practice = practice, group = group, role = role)
            groupPracticeAccessRepository.save(access)
        }
    }

    @Transactional
    fun revokePracticeFromGroup(ownerId: Int, practiceId: Int, groupId: Int) {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        if (practice.owner?.id != ownerId) throw UnauthorizedException("Only the owner can revoke access")

        val access = groupPracticeAccessRepository.findByPracticeIdAndGroupId(practiceId, groupId)
            ?: throw ResourceNotFoundException("No shared access found for this group and practice")

        groupPracticeAccessRepository.delete(access)
    }
}
