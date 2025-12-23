package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.CollaboratorDTO
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.AccessRole
import org.sportstechsolutions.apitacticsapp.model.CollaboratorType
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

    // --- NEW: RETRIEVAL LOGIC ---
    @Transactional(readOnly = true)
    fun getSessionCollaborators(ownerId: Int, sessionId: Int): List<CollaboratorDTO> {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        val owner = session.owner
            ?: throw IllegalStateException("Session has no owner")

        if (owner.id != ownerId) {
            throw UnauthorizedException("Access denied")
        }

        val collaborators = mutableListOf<CollaboratorDTO>()

        // --- USER COLLABORATORS ---
        val userAccess = userSessionAccessRepository.findBySessionId(sessionId)
            .mapNotNull { access ->
                val user = access.user ?: return@mapNotNull null

                // skip owner if somehow present
                if (user.id == owner.id) return@mapNotNull null

                CollaboratorDTO(
                    id = user.id,
                    name = user.email,
                    type = CollaboratorType.USER,
                    role = access.role
                )
            }

        // --- GROUP COLLABORATORS ---
        val groupAccess = groupSessionAccessRepository.findBySessionId(sessionId)
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


    // --- USER SHARING ---
    @Transactional
    fun shareSessionWithUser(ownerId: Int, sessionId: Int, targetUserId: Int, role: AccessRole) {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        if (session.owner?.id != ownerId) {
            throw UnauthorizedException("Only the session owner can share this session")
        }

        val user = userRepository.findById(targetUserId)
            .orElseThrow { ResourceNotFoundException("User not found") }

        // Manual uniqueness check
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
            ?: throw ResourceNotFoundException("No shared access found")

        userSessionAccessRepository.delete(access)
    }

    // --- GROUP SHARING ---
    @Transactional
    fun shareSessionWithGroup(ownerId: Int, sessionId: Int, groupId: Int, role: AccessRole) {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        if (session.owner?.id != ownerId) {
            throw UnauthorizedException("Only the session owner can share this session")
        }

        val group = groupRepository.findById(groupId)
            .orElseThrow { ResourceNotFoundException("Group not found") }

        // Manual uniqueness check
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
            ?: throw ResourceNotFoundException("No shared access found")

        groupSessionAccessRepository.delete(access)
    }
}
