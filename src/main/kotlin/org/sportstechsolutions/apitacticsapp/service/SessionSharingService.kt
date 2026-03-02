package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.CollaboratorDTO
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.*
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

    @Transactional(readOnly = true)
    fun getSessionCollaborators(ownerId: Int, sessionId: Int): List<CollaboratorDTO> {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Session not found") }

        if (session.owner?.id != ownerId) {
            throw UnauthorizedException("Only the owner can view collaborators")
        }

        val collaborators = mutableListOf<CollaboratorDTO>()

        // User Collaborators
        val userAccess = userSessionAccessRepository.findBySessionId(sessionId)
            .mapNotNull { access ->
                val user = access.user ?: return@mapNotNull null
                CollaboratorDTO(user.id, user.email, CollaboratorType.USER, access.role)
            }

        // Group Collaborators
        val groupAccess = groupSessionAccessRepository.findBySessionId(sessionId)
            .mapNotNull { access ->
                val group = access.group ?: return@mapNotNull null
                CollaboratorDTO(group.id, group.name, CollaboratorType.GROUP, access.role)
            }

        collaborators.addAll(userAccess)
        collaborators.addAll(groupAccess)
        return collaborators
    }

    @Transactional
    fun shareSessionWithUser(ownerId: Int, sessionId: Int, targetUserId: Int, role: AccessRole) {
        if (ownerId == targetUserId) throw IllegalArgumentException("You cannot share an item with yourself")

        val session = sessionRepository.findById(sessionId).orElseThrow { ResourceNotFoundException("Session not found") }
        if (session.owner?.id != ownerId) throw UnauthorizedException("Not the owner")

        val user = userRepository.findById(targetUserId).orElseThrow { ResourceNotFoundException("User not found") }

        val existingAccess = userSessionAccessRepository.findByUserIdAndSessionId(targetUserId, sessionId)
        if (existingAccess != null) {
            existingAccess.role = role
            userSessionAccessRepository.save(existingAccess)
        } else {
            userSessionAccessRepository.save(UserSessionAccess(user = user, session = session, role = role))
        }
    }

    @Transactional
    fun revokeSessionFromUser(ownerId: Int, sessionId: Int, targetUserId: Int) {
        val session = sessionRepository.findById(sessionId).orElseThrow { ResourceNotFoundException("Session not found") }
        if (session.owner?.id != ownerId) throw UnauthorizedException("Not the owner")

        val access = userSessionAccessRepository.findByUserIdAndSessionId(targetUserId, sessionId)
            ?: throw ResourceNotFoundException("Access record not found")

        userSessionAccessRepository.delete(access)
    }

    @Transactional
    fun shareSessionWithGroup(ownerId: Int, sessionId: Int, groupId: Int, role: AccessRole) {
        val session = sessionRepository.findById(sessionId).orElseThrow { ResourceNotFoundException("Session not found") }
        if (session.owner?.id != ownerId) throw UnauthorizedException("Not the owner")

        val group = groupRepository.findById(groupId).orElseThrow { ResourceNotFoundException("Group not found") }

        val existingAccess = groupSessionAccessRepository.findBySessionIdAndGroupId(sessionId, groupId)
        if (existingAccess != null) {
            existingAccess.role = role
            groupSessionAccessRepository.save(existingAccess)
        } else {
            groupSessionAccessRepository.save(GroupSessionAccess(session = session, group = group, role = role))
        }
    }

    @Transactional
    fun revokeSessionFromGroup(ownerId: Int, sessionId: Int, groupId: Int) {
        val session = sessionRepository.findById(sessionId).orElseThrow { ResourceNotFoundException("Session not found") }
        if (session.owner?.id != ownerId) throw UnauthorizedException("Not the owner")

        val access = groupSessionAccessRepository.findBySessionIdAndGroupId(sessionId, groupId)
            ?: throw ResourceNotFoundException("Access record not found")

        groupSessionAccessRepository.delete(access)
    }
}