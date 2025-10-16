package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PracticeService(
    private val practiceRepository: PracticeRepository,
    private val userPracticeAccessRepository: UserPracticeAccessRepository,
    private val groupPracticeAccessRepository: GroupPracticeAccessRepository,
    private val entityMappers: EntityMappers
) {

    // ------------------------------------------
    // Tabbed view (personal, user shared, group shared)
    // ------------------------------------------
    @Transactional(readOnly = true)
    fun getPracticesForTabs(userId: Int): TabbedResponse<PracticeResponse> {
        val personal = practiceRepository.findByOwnerId(userId)
            .map { entityMappers.loadFullPractice(it) }

        val userShared = userPracticeAccessRepository.findByUserId(userId)
            .filter { it.role != AccessRole.NONE }
            .mapNotNull { it.practice }
            .map { entityMappers.loadFullPractice(it) }

        val groupShared = groupPracticeAccessRepository.findByGroupMemberId(userId)
            .mapNotNull { it.practice }
            .distinct()
            .map { entityMappers.loadFullPractice(it) }

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
    fun createPractice(userId: Int, request: PracticeRequest): PracticeResponse {
        val practice = Practice(
            name = request.name,
            description = request.description,
            is_premade = request.isPremade,
            owner = User(id = userId)
        )

        val sessions = request.sessions.map { entityMappers.toSession(it, practice) }
        sessions.forEach { it.practices.add(practice) }
        practice.sessions.addAll(sessions)

        val saved = practiceRepository.save(practice)
        return entityMappers.loadFullPractice(saved)
    }

    @Transactional
    fun updatePractice(userId: Int, practiceId: Int, request: PracticeRequest, groupId: Int? = null): PracticeResponse {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        val role = if (practice.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForPractice(userId, practiceId, groupId)

        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to edit this practice")

        practice.name = request.name
        practice.description = request.description
        practice.is_premade = request.isPremade
        practice.sessions.forEach { it.practices.remove(practice) }
        practice.sessions.clear()

        val newSessions = request.sessions.map { entityMappers.toSession(it, practice) }
        newSessions.forEach { it.practices.add(practice) }
        practice.sessions.addAll(newSessions)

        val updated = practiceRepository.save(practice)
        return entityMappers.loadFullPractice(updated)
    }

    @Transactional
    fun deletePractice(userId: Int, practiceId: Int, groupId: Int? = null) {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        val role = if (practice.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForPractice(userId, practiceId, groupId)

        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to delete this practice")

        practiceRepository.delete(practice)
    }

    @Transactional(readOnly = true)
    fun getPracticeById(practiceId: Int, userId: Int, groupId: Int? = null): PracticeResponse {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        val role = if (practice.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForPractice(userId, practiceId, groupId)

        if (role == AccessRole.NONE) throw UnauthorizedException("You do not have access to this practice")
        return entityMappers.loadFullPractice(practice)
    }

    // ------------------------------------------
    // Access role resolver
    // ------------------------------------------
    fun getUserRoleForPractice(userId: Int, practiceId: Int, groupId: Int? = null): AccessRole {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        if (practice.owner?.id == userId) return AccessRole.OWNER

        val userAccess = userPracticeAccessRepository.findByUserIdAndPracticeId(userId, practiceId)
        if (userAccess != null) return userAccess.role

        val groupAccess = if (groupId != null) {
            groupPracticeAccessRepository.findByPracticeIdAndGroupId(practiceId, groupId)
                ?.takeIf { it.group?.members?.any { m -> m.id == userId } == true }
        } else {
            groupPracticeAccessRepository.findByPracticeId(practiceId)
                ?.firstOrNull { it.group?.members?.any { m -> m.id == userId } == true }
        }

        return groupAccess?.role ?: AccessRole.NONE
    }
}
