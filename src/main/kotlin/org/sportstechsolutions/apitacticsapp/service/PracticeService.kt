package org.sportstechsolutions.apitacticsapp.service

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
class PracticeService(
    private val practiceRepository: PracticeRepository,
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val userPracticeAccessRepository: UserPracticeAccessRepository,
    private val groupPracticeAccessRepository: GroupPracticeAccessRepository,
    private val entityMappers: EntityMappers,
) {

    @Transactional(readOnly = true)
    fun getPracticesForTabs(userId: Int, pageable: Pageable): TabbedResponse<PracticeSummaryResponse> {
        val personalPage = practiceRepository.findByOwnerId(userId, pageable)
        val personalPaged = PagedResponse(
            content = personalPage.content.map { entityMappers.toPracticeSummary(it, AccessRole.OWNER, userId) },
            pageNumber = personalPage.number,
            pageSize = personalPage.size,
            totalElements = personalPage.totalElements,
            totalPages = personalPage.totalPages,
            isLast = personalPage.isLast
        )

        // DB level filter prevents pagination count mismatch
        val userSharedPage = userPracticeAccessRepository.findByUserIdAndRoleNot(userId, AccessRole.NONE, pageable)
        val userSharedPaged = PagedResponse(
            content = userSharedPage.content.mapNotNull { access ->
                access.practice?.let { p -> entityMappers.toPracticeSummary(p, access.role, userId) }
            },
            pageNumber = userSharedPage.number,
            pageSize = userSharedPage.size,
            totalElements = userSharedPage.totalElements,
            totalPages = userSharedPage.totalPages,
            isLast = userSharedPage.isLast
        )

        val groupSharedPage = groupPracticeAccessRepository.findByGroupMemberId(userId, pageable)
        val groupSharedPaged = PagedResponse(
            content = groupSharedPage.content.mapNotNull { access ->
                access.practice?.let { p -> entityMappers.toPracticeSummary(p, access.role, userId) }
            },
            pageNumber = groupSharedPage.number,
            pageSize = groupSharedPage.size,
            totalElements = groupSharedPage.totalElements,
            totalPages = groupSharedPage.totalPages,
            isLast = groupSharedPage.isLast
        )

        return TabbedResponse(personalPaged, userSharedPaged, groupSharedPaged)
    }

    @Transactional(readOnly = true)
    fun searchPractices(userId: Int, request: PracticeSearchRequest, pageable: Pageable): PagedResponse<PracticeSummaryResponse> {
        // Build Spec using DB-level EXISTS subqueries, removing the in-memory array
        val spec = SearchSpecifications.buildPracticeSearchSpec(request, userId)

        val finalPageable = if (request.sortBy == SortBy.VIEWS) {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "viewCount"))
        } else {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "id"))
        }

        val practicePage = practiceRepository.findAll(spec, finalPageable)
        val practiceIds = practicePage.content.mapNotNull { it.id }

        // Bulk fetch roles to prevent N+1 queries during DTO mapping
        val userRoleMap = if (userId != 0 && practiceIds.isNotEmpty()) {
            userPracticeAccessRepository.findRolesForUserInPractices(userId, practiceIds).associate { it.practiceId to it.role }
        } else emptyMap()

        val groupRoleMap = if (userId != 0 && practiceIds.isNotEmpty()) {
            groupPracticeAccessRepository.findRolesForUserInPractices(userId, practiceIds).associate { it.practiceId to it.role }
        } else emptyMap()

        val content = practicePage.content.map { practice ->
            val practiceId = practice.id ?: 0

            val role = when {
                userId == 0 -> AccessRole.VIEWER
                practice.owner?.id == userId -> AccessRole.OWNER
                userRoleMap.containsKey(practiceId) -> userRoleMap[practiceId]!!
                groupRoleMap.containsKey(practiceId) -> groupRoleMap[practiceId]!!
                practice.isPublic || practice.isPremade -> AccessRole.VIEWER
                else -> AccessRole.NONE
            }
            entityMappers.toPracticeSummary(practice, role, userId)
        }

        return PagedResponse(
            content = content,
            pageNumber = practicePage.number,
            pageSize = practicePage.size,
            totalElements = practicePage.totalElements,
            totalPages = practicePage.totalPages,
            isLast = practicePage.isLast
        )
    }

    @Transactional
    fun createPractice(userId: Int, request: PracticeRequest): PracticeResponse {
        val user = userRepository.findById(userId).orElseThrow { ResourceNotFoundException("User not found") }

        val practice = Practice(
            name = request.name,
            description = request.description,
            isPremade = request.isPremade,
            isPublic = request.isPublic,
            owner = user,
            phaseOfPlay = request.phaseOfPlay,
            ballContext = request.ballContext,
            drillFormat = request.drillFormat,
            minPlayers = request.minPlayers,
            maxPlayers = request.maxPlayers,
            durationMinutes = request.durationMinutes,
            areaSize = request.areaSize,
            targetAgeLevel = request.targetAgeLevel,
            tacticalActions = request.tacticalActions.toMutableSet(),
            qualityMakers = request.qualityMakers.toMutableSet()
        )

        val sessionsToAttach = request.sessions.map { dto ->
            val sessionId = dto.id ?: throw IllegalArgumentException("Session ID required")
            sessionRepository.findById(sessionId)
                .orElseThrow { ResourceNotFoundException("Session $sessionId not found") }
        }

        sessionsToAttach.forEach { session -> session.practices.add(practice) }
        practice.sessions.addAll(sessionsToAttach)

        val saved = practiceRepository.save(practice)
        return entityMappers.loadFullPractice(saved, AccessRole.OWNER, userId)
    }

    @Transactional
    fun updatePractice(userId: Int, practiceId: Int, request: PracticeRequest, groupId: Int? = null): PracticeResponse {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        // Prevent redundant database fetch
        val role = getUserRoleForPractice(userId, practice, groupId)

        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to edit this practice")

        practice.name = request.name
        practice.description = request.description
        practice.isPremade = request.isPremade
        practice.isPublic = request.isPublic
        practice.phaseOfPlay = request.phaseOfPlay
        practice.ballContext = request.ballContext
        practice.drillFormat = request.drillFormat
        practice.minPlayers = request.minPlayers
        practice.maxPlayers = request.maxPlayers
        practice.durationMinutes = request.durationMinutes
        practice.areaSize = request.areaSize
        practice.targetAgeLevel = request.targetAgeLevel

        practice.tacticalActions.clear()
        practice.tacticalActions.addAll(request.tacticalActions)

        practice.qualityMakers.clear()
        practice.qualityMakers.addAll(request.qualityMakers)

        practice.sessions.forEach { session -> session.practices.remove(practice) }
        practice.sessions.clear()

        val sessionsToAttach = request.sessions.map { dto ->
            val sessionId = dto.id ?: throw IllegalArgumentException("Session ID required")
            sessionRepository.findById(sessionId)
                .orElseThrow { ResourceNotFoundException("Session $sessionId not found") }
        }

        sessionsToAttach.forEach { session -> session.practices.add(practice) }
        practice.sessions.addAll(sessionsToAttach)

        val updated = practiceRepository.save(practice)
        return entityMappers.loadFullPractice(updated, role, userId)
    }

    @Transactional
    fun deletePractice(userId: Int, practiceId: Int, groupId: Int? = null) {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        val role = getUserRoleForPractice(userId, practice, groupId)

        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to delete this practice")

        practice.sessions.forEach { session -> session.practices.remove(practice) }

        userPracticeAccessRepository.deleteAllByPractice(practice)
        practiceRepository.delete(practice)
    }

    @Transactional
    fun getPracticeById(practiceId: Int, userId: Int, groupId: Int? = null): PracticeResponse {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        val role = getUserRoleForPractice(userId, practice, groupId)

        if (role == AccessRole.NONE) throw UnauthorizedException("You do not have access to this practice")

        practice.viewCount += 1
        practiceRepository.save(practice)

        return entityMappers.loadFullPractice(practice, role, userId)
    }

    @Transactional
    fun toggleFavorite(userId: Int, practiceId: Int): Boolean {
        val role = getUserRoleForPractice(userId, practiceId)
        if (role == AccessRole.NONE) throw UnauthorizedException("You do not have access to this practice")

        val isAlreadyFavorite = practiceRepository.isPracticeFavoritedByUser(practiceId, userId)
        if (isAlreadyFavorite) {
            practiceRepository.removeFavorite(practiceId, userId)
        } else {
            practiceRepository.addFavorite(practiceId, userId)
        }
        return !isAlreadyFavorite
    }

    // Main resolver: operates on the entity to prevent double fetching
    fun getUserRoleForPractice(userId: Int, practice: Practice, groupId: Int? = null): AccessRole {
        val practiceId = practice.id ?: return AccessRole.NONE
        if (practice.owner?.id == userId) return AccessRole.OWNER

        val userAccess = userPracticeAccessRepository.findByUserIdAndPracticeId(userId, practiceId)
        if (userAccess != null) return userAccess.role

        val groupRole = groupPracticeAccessRepository.findRoleForUserInPractice(userId, practiceId)
        if (groupRole != null) return groupRole

        return if (practice.isPublic || practice.isPremade) AccessRole.VIEWER else AccessRole.NONE
    }

    // Fallback resolver
    fun getUserRoleForPractice(userId: Int, practiceId: Int, groupId: Int? = null): AccessRole {
        val practice = practiceRepository.findById(practiceId).orElseThrow { ResourceNotFoundException("Practice not found") }
        return getUserRoleForPractice(userId, practice, groupId)
    }
}