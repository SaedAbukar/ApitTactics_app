package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.*
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

    // ------------------------------------------------------------
    // Tabbed view (Returns Paginated Summaries)
    // ------------------------------------------------------------
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

        val userSharedPage = userPracticeAccessRepository.findByUserId(userId, pageable)
        val userSharedPaged = PagedResponse(
            content = userSharedPage.content.filter { it.role != AccessRole.NONE }.mapNotNull { access ->
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

    // ------------------------------------------------------------
    // Advanced Search
    // ------------------------------------------------------------
    @Transactional(readOnly = true)
    fun searchPractices(userId: Int, request: PracticeSearchRequest, pageable: Pageable): PagedResponse<PracticeSummaryResponse> {
        // GUEST SAFETY: Use emptySet() if userId is 0 to avoid DB errors, and use .toSet() for type safety
        val accessibleIds: Set<Int> = if (userId == 0) {
            emptySet()
        } else {
            practiceRepository.findAllAccessiblePracticeIds(userId).toSet()
        }

        val spec = org.sportstechsolutions.apitacticsapp.repository.specifications.SearchSpecifications.buildPracticeSearchSpec(request, accessibleIds, userId)

        val finalPageable = if (request.sortBy == SortBy.VIEWS) {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "viewCount"))
        } else {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "id"))
        }

        val practicePage = practiceRepository.findAll(spec, finalPageable)
        val content = practicePage.content.map { practice ->
            val role = getUserRoleForPractice(userId, practice.id ?: 0)
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

    // ------------------------------------------------------------
    // CRUD Operations
    // ------------------------------------------------------------
    @Transactional
    fun createPractice(userId: Int, request: PracticeRequest): PracticeResponse {
        val user = userRepository.findById(userId).orElseThrow { ResourceNotFoundException("User not found") }

        val practice = Practice(
            name = request.name,
            description = request.description,
            isPremade = request.isPremade,
            isPublic = request.isPublic, // Handled new visibility flag
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
            dto.id?.let { sessionId ->
                sessionRepository.findById(sessionId)
                    .orElseThrow { ResourceNotFoundException("Session $sessionId not found") }
            } ?: run {
                entityMappers.toSession(dto, practice)
            }
        }

        // BIDIRECTIONAL SYNC: Ensure Sessions know they belong to this Practice
        sessionsToAttach.forEach { session ->
            session.practices.add(practice)
        }
        practice.sessions.addAll(sessionsToAttach)

        val saved = practiceRepository.save(practice)
        return entityMappers.loadFullPractice(saved, AccessRole.OWNER, userId)
    }

    @Transactional
    fun updatePractice(userId: Int, practiceId: Int, request: PracticeRequest, groupId: Int? = null): PracticeResponse {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        val role = if (practice.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForPractice(userId, practiceId, groupId)

        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to edit this practice")

        // Update fields
        practice.name = request.name
        practice.description = request.description
        practice.isPremade = request.isPremade
        practice.isPublic = request.isPublic // Update visibility
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

        // SYNC: Detach old sessions from both sides
        practice.sessions.forEach { session ->
            session.practices.remove(practice)
        }
        practice.sessions.clear()

        val sessionsToAttach = request.sessions.map { dto ->
            dto.id?.let { sessionId ->
                sessionRepository.findById(sessionId)
                    .orElseThrow { ResourceNotFoundException("Session $sessionId not found") }
            } ?: run {
                entityMappers.toSession(dto, practice)
            }
        }

        // SYNC: Re-attach new sessions to both sides
        sessionsToAttach.forEach { session ->
            session.practices.add(practice)
        }
        practice.sessions.addAll(sessionsToAttach)

        val updated = practiceRepository.save(practice)
        return entityMappers.loadFullPractice(updated, role, userId)
    }

    @Transactional
    fun deletePractice(userId: Int, practiceId: Int, groupId: Int? = null) {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        val role = if (practice.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForPractice(userId, practiceId, groupId)

        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to delete this practice")

        // BIDIRECTIONAL CLEANUP: Remove the reference from sessions before deleting
        practice.sessions.forEach { session ->
            session.practices.remove(practice)
        }

        userPracticeAccessRepository.deleteAllByPractice(practice)
        practiceRepository.delete(practice)
    }

    @Transactional
    fun getPracticeById(practiceId: Int, userId: Int, groupId: Int? = null): PracticeResponse {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        val role = if (practice.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForPractice(userId, practiceId, groupId)

        if (role == AccessRole.NONE) throw UnauthorizedException("You do not have access to this practice")

        practice.viewCount += 1
        practiceRepository.save(practice)

        return entityMappers.loadFullPractice(practice, role, userId)
    }

    // ------------------------------------------------------------
    // Favorites & Accessibility
    // ------------------------------------------------------------
    @Transactional
    fun toggleFavorite(userId: Int, practiceId: Int): Boolean {
        val practice = practiceRepository.findById(practiceId).orElseThrow { ResourceNotFoundException("Practice not found") }
        val user = userRepository.findById(userId).orElseThrow { ResourceNotFoundException("User not found") }

        val role = if (practice.owner?.id == userId) AccessRole.OWNER else getUserRoleForPractice(userId, practiceId)
        if (role == AccessRole.NONE) throw UnauthorizedException("You do not have access to this practice")

        val isAlreadyFavorite = practice.favoritedByUsers.any { it.id == userId }
        if (isAlreadyFavorite) {
            practice.favoritedByUsers.removeIf { it.id == userId }
        } else {
            practice.favoritedByUsers.add(user)
        }

        practiceRepository.save(practice)
        return !isAlreadyFavorite
    }

    fun getUserRoleForPractice(userId: Int, practiceId: Int, groupId: Int? = null): AccessRole {
        val practice = practiceRepository.findById(practiceId).orElseThrow { ResourceNotFoundException("Practice not found") }

        if (practice.owner?.id == userId) return AccessRole.OWNER

        val userAccess = userPracticeAccessRepository.findByUserIdAndPracticeId(userId, practiceId)
        if (userAccess != null) return userAccess.role

        val groupAccess = if (groupId != null) {
            groupPracticeAccessRepository.findByPracticeIdAndGroupId(practiceId, groupId)
                ?.takeIf { it.group?.members?.any { m -> m.id == userId } == true }
        } else {
            groupPracticeAccessRepository.findByPracticeId(practiceId)
                .firstOrNull { it.group?.members?.any { m -> m.id == userId } == true }
        }
        if (groupAccess != null) return groupAccess.role

        return if (practice.isPublic || practice.isPremade) AccessRole.VIEWER else AccessRole.NONE
    }
}