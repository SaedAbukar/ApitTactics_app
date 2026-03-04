package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.CollaboratorDTO
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
        // Fast ownership check without loading the entity
        if (!sessionRepository.existsByIdAndOwnerId(sessionId, ownerId)) {
            throw UnauthorizedException("Only the owner can view collaborators or session does not exist")
        }

        val collaborators = mutableListOf<CollaboratorDTO>()

        // Uses JOIN FETCH to get all users in 1 query
        val userAccess = userSessionAccessRepository.findAllWithUserBySessionId(sessionId).mapNotNull { access ->
            val user = access.user ?: return@mapNotNull null
            CollaboratorDTO(user.id, user.email, CollaboratorType.USER, access.role)
        }

        // Uses JOIN FETCH to get all groups in 1 query
        val groupAccess = groupSessionAccessRepository.findAllWithGroupBySessionId(sessionId).mapNotNull { access ->
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
        if (!sessionRepository.existsByIdAndOwnerId(sessionId, ownerId)) {
            throw UnauthorizedException("You do not have permission to share this session")
        }

        val existingAccess = userSessionAccessRepository.findByUserIdAndSessionId(targetUserId, sessionId)
        if (existingAccess != null) {
            existingAccess.role = role
            // Hibernate auto-updates managed entities on transaction commit, no .save() needed
        } else {
            // getReferenceById creates a Proxy. No SELECT queries are executed!
            val sessionProxy = sessionRepository.getReferenceById(sessionId)
            val userProxy = userRepository.getReferenceById(targetUserId)
            userSessionAccessRepository.save(UserSessionAccess(user = userProxy, session = sessionProxy, role = role))
        }
    }

    @Transactional
    fun revokeSessionFromUser(ownerId: Int, sessionId: Int, targetUserId: Int) {
        if (!sessionRepository.existsByIdAndOwnerId(sessionId, ownerId)) {
            throw UnauthorizedException("You do not have permission to revoke access to this session")
        }

        // Single DB command: DELETE WHERE userId = ? AND sessionId = ?
        userSessionAccessRepository.deleteByUserIdAndSessionId(targetUserId, sessionId)
    }

    @Transactional
    fun shareSessionWithGroup(ownerId: Int, sessionId: Int, groupId: Int, role: AccessRole) {
        if (!sessionRepository.existsByIdAndOwnerId(sessionId, ownerId)) {
            throw UnauthorizedException("You do not have permission to share this session")
        }

        val existingAccess = groupSessionAccessRepository.findBySessionIdAndGroupId(sessionId, groupId)
        if (existingAccess != null) {
            existingAccess.role = role
        } else {
            // getReferenceById creates a Proxy. No SELECT queries are executed!
            val sessionProxy = sessionRepository.getReferenceById(sessionId)
            val groupProxy = groupRepository.getReferenceById(groupId)
            groupSessionAccessRepository.save(GroupSessionAccess(session = sessionProxy, group = groupProxy, role = role))
        }
    }

    @Transactional
    fun revokeSessionFromGroup(ownerId: Int, sessionId: Int, groupId: Int) {
        if (!sessionRepository.existsByIdAndOwnerId(sessionId, ownerId)) {
            throw UnauthorizedException("You do not have permission to revoke access to this session")
        }

        // Single DB command: DELETE WHERE groupId = ? AND sessionId = ?
        groupSessionAccessRepository.deleteByGroupIdAndSessionId(groupId, sessionId)
    }
}