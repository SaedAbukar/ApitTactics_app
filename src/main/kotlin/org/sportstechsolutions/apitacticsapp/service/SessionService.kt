package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.*
import org.sportstechsolutions.apitacticsapp.dtos.EntityMappers
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SessionService(
    private val sessionRepository: SessionRepository,
    private val userSessionAccessRepository: UserSessionAccessRepository,
    private val groupSessionAccessRepository: GroupSessionAccessRepository,
    private val entityMappers: EntityMappers
) {

    @Transactional(readOnly = true)
    fun getSessionsForTabs(userId: Int): TabbedResponse<SessionResponse> {
        val personal = sessionRepository.findByOwnerId(userId).map { entityMappers.loadFullSession(it) }

        val userShared = userSessionAccessRepository.findByUserId(userId)
            .filter { it.role != AccessRole.NONE }
            .mapNotNull { it.session }
            .map { entityMappers.loadFullSession(it) }

        val groupShared = groupSessionAccessRepository.findByGroupMemberId(userId)
            .mapNotNull { it.session }
            .distinct()
            .map { entityMappers.loadFullSession(it) }

        return TabbedResponse(
            personalItems = personal,
            userSharedItems = userShared,
            groupSharedItems = groupShared
        )
    }

    @Transactional
    fun createSession(userId: Int, request: SessionRequest): SessionResponse {
        val owner = User(id = userId)
        val session = entityMappers.toSession(request, owner)
        val saved = sessionRepository.save(session)
        return entityMappers.loadFullSession(saved)
    }

    @Transactional
    fun updateSession(userId: Int, sessionId: Int, request: SessionRequest, groupId: Int? = null): SessionResponse {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        val role = if (session.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForSession(userId, sessionId, groupId)

        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to edit this session")

        val owner = session.owner ?: throw IllegalStateException("Session must have an owner")
        session.name = request.name
        session.description = request.description
        session.steps.clear()
        session.steps.addAll(request.steps.map { entityMappers.toStep(it, session, owner) })

        val updated = sessionRepository.save(session)
        return entityMappers.loadFullSession(updated)
    }

    @Transactional
    fun deleteSession(userId: Int, sessionId: Int, groupId: Int? = null) {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        val role = if (session.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForSession(userId, sessionId, groupId)

        if (!role.canEdit()) throw UnauthorizedException("You do not have permission to delete this session")

        userSessionAccessRepository.deleteAllBySession(session)
        sessionRepository.delete(session)
    }

    @Transactional(readOnly = true)
    fun getSessionById(sessionId: Int, userId: Int, groupId: Int? = null): SessionResponse {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        val role = if (session.owner?.id == userId) AccessRole.OWNER
        else getUserRoleForSession(userId, sessionId, groupId)

        if (role == AccessRole.NONE) throw UnauthorizedException("You do not have access to this session")
        return entityMappers.loadFullSession(session)
    }

    fun getUserRoleForSession(userId: Int, sessionId: Int, groupId: Int? = null): AccessRole {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        if (session.owner?.id == userId) return AccessRole.OWNER

        val userAccess = userSessionAccessRepository.findByUserIdAndSessionId(userId, sessionId)
        if (userAccess != null) return userAccess.role

        val groupAccess = if (groupId != null) {
            groupSessionAccessRepository.findBySessionIdAndGroupId(sessionId, groupId)
                ?.takeIf { it.group?.members?.any { m -> m.id == userId } == true }
        } else {
            groupSessionAccessRepository.findBySessionId(sessionId)
                ?.firstOrNull { it.group?.members?.any { m -> m.id == userId } == true }
        }

        return groupAccess?.role ?: AccessRole.NONE
    }
}
