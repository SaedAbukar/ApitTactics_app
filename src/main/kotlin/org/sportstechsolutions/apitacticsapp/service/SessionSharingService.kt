package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.AccessRole
import org.sportstechsolutions.apitacticsapp.model.GroupSessionAccess
import org.sportstechsolutions.apitacticsapp.model.UserSessionAccess
import org.sportstechsolutions.apitacticsapp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SessionSharingService(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val groupRepository: UserGroupRepository,
    private val userSessionAccessRepository: UserSessionAccessRepository,
    private val groupSessionAccessRepository: GroupSessionAccessRepository
) {

    // -----------------------------
    // User sharing
    // -----------------------------
    @Transactional
    fun shareSessionWithUser(ownerId: Int, sessionId: Int, targetUserId: Int, role: AccessRole) {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        if (session.owner?.id != ownerId) {
            throw UnauthorizedException("Only the session owner can share this session")
        }

        val user = userRepository.findById(targetUserId)
            .orElseThrow { ResourceNotFoundException("User not found") }

        val existingAccess = userSessionAccessRepository.findByUserIdAndSessionId(targetUserId, sessionId)
        if (existingAccess != null) {
            existingAccess.role = role
            userSessionAccessRepository.save(existingAccess)
        } else {
            val access = UserSessionAccess(user = user, session = session, role = role)
            userSessionAccessRepository.save(access)
        }
    }

    @Transactional
    fun revokeSessionFromUser(ownerId: Int, sessionId: Int, targetUserId: Int) {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        if (session.owner?.id != ownerId) {
            throw UnauthorizedException("Only the session owner can revoke access")
        }

        val access = userSessionAccessRepository.findByUserIdAndSessionId(targetUserId, sessionId)
            ?: throw ResourceNotFoundException("No shared access found for this user and session")
        userSessionAccessRepository.delete(access)
    }

    // -----------------------------
    // Group sharing
    // -----------------------------
    @Transactional
    fun shareSessionWithGroup(ownerId: Int, sessionId: Int, groupId: Int, role: AccessRole) {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        if (session.owner?.id != ownerId) {
            throw UnauthorizedException("Only the session owner can share this session")
        }

        val group = groupRepository.findById(groupId)
            .orElseThrow { ResourceNotFoundException("Group not found") }

        val existingAccess = groupSessionAccessRepository.findBySessionIdAndGroupId(sessionId, groupId)
        if (existingAccess != null) {
            existingAccess.role = role
            groupSessionAccessRepository.save(existingAccess)
        } else {
            val access = GroupSessionAccess(session = session, group = group, role = role)
            groupSessionAccessRepository.save(access)
        }
    }

    @Transactional
    fun revokeSessionFromGroup(ownerId: Int, sessionId: Int, groupId: Int) {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        if (session.owner?.id != ownerId) {
            throw UnauthorizedException("Only the session owner can revoke access")
        }

        val access = groupSessionAccessRepository.findBySessionIdAndGroupId(sessionId, groupId)
            ?: throw ResourceNotFoundException("No shared access found for this group and session")
        groupSessionAccessRepository.delete(access)
    }
}
