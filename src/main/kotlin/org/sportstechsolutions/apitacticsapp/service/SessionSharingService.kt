package org.sportstechsolutions.apitacticsapp.service

import org.slf4j.LoggerFactory
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

    private val log = LoggerFactory.getLogger(SessionSharingService::class.java)

    @Transactional(readOnly = true)
    fun getSessionCollaborators(ownerId: Int, sessionId: Int): List<CollaboratorDTO> {
        log.debug("Fetching collaborators for Session ID: $sessionId requested by User ID: $ownerId")

        // Fast ownership check without loading the entity
        if (!sessionRepository.existsByIdAndOwnerId(sessionId, ownerId)) {
            log.warn("Access denied: User ID $ownerId is not the owner of Session ID $sessionId")
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

        log.debug("Successfully retrieved ${collaborators.size} collaborators for Session ID: $sessionId")
        return collaborators
    }

    @Transactional
    fun shareSessionWithUser(ownerId: Int, sessionId: Int, targetUserId: Int, role: AccessRole) {
        log.info("Attempting to share Session ID: $sessionId with User ID: $targetUserId by Owner ID: $ownerId")

        if (ownerId == targetUserId) {
            log.warn("Share rejected: User ID $ownerId attempted to share Session ID $sessionId with themselves")
            throw IllegalArgumentException("You cannot share an item with yourself")
        }

        if (!sessionRepository.existsByIdAndOwnerId(sessionId, ownerId)) {
            log.warn("Share rejected: User ID $ownerId is not the owner of Session ID $sessionId")
            throw UnauthorizedException("You do not have permission to share this session")
        }

        val existingAccess = userSessionAccessRepository.findByUserIdAndSessionId(targetUserId, sessionId)
        if (existingAccess != null) {
            log.debug("Updating existing access role for User ID $targetUserId on Session ID $sessionId to $role")
            existingAccess.role = role
            // Hibernate auto-updates managed entities on transaction commit, no .save() needed
        } else {
            log.debug("Creating new access grant for User ID $targetUserId on Session ID $sessionId with role $role")
            // getReferenceById creates a Proxy. No SELECT queries are executed!
            val sessionProxy = sessionRepository.getReferenceById(sessionId)
            val userProxy = userRepository.getReferenceById(targetUserId)
            userSessionAccessRepository.save(UserSessionAccess(user = userProxy, session = sessionProxy, role = role))
        }
        log.info("Successfully shared Session ID: $sessionId with User ID: $targetUserId")
    }

    @Transactional
    fun revokeSessionFromUser(ownerId: Int, sessionId: Int, targetUserId: Int) {
        log.info("Attempting to revoke access to Session ID: $sessionId from User ID: $targetUserId by Owner ID: $ownerId")

        if (!sessionRepository.existsByIdAndOwnerId(sessionId, ownerId)) {
            log.warn("Revoke rejected: User ID $ownerId is not the owner of Session ID $sessionId")
            throw UnauthorizedException("You do not have permission to revoke access to this session")
        }

        // Single DB command: DELETE WHERE userId = ? AND sessionId = ?
        userSessionAccessRepository.deleteByUserIdAndSessionId(targetUserId, sessionId)
        log.info("Successfully revoked access to Session ID: $sessionId for User ID: $targetUserId")
    }

    @Transactional
    fun shareSessionWithGroup(ownerId: Int, sessionId: Int, groupId: Int, role: AccessRole) {
        log.info("Attempting to share Session ID: $sessionId with Group ID: $groupId by Owner ID: $ownerId")

        if (!sessionRepository.existsByIdAndOwnerId(sessionId, ownerId)) {
            log.warn("Share rejected: User ID $ownerId is not the owner of Session ID $sessionId")
            throw UnauthorizedException("You do not have permission to share this session")
        }

        val existingAccess = groupSessionAccessRepository.findBySessionIdAndGroupId(sessionId, groupId)
        if (existingAccess != null) {
            log.debug("Updating existing access role for Group ID $groupId on Session ID $sessionId to $role")
            existingAccess.role = role
        } else {
            log.debug("Creating new access grant for Group ID $groupId on Session ID $sessionId with role $role")
            // getReferenceById creates a Proxy. No SELECT queries are executed!
            val sessionProxy = sessionRepository.getReferenceById(sessionId)
            val groupProxy = groupRepository.getReferenceById(groupId)
            groupSessionAccessRepository.save(GroupSessionAccess(session = sessionProxy, group = groupProxy, role = role))
        }
        log.info("Successfully shared Session ID: $sessionId with Group ID: $groupId")
    }

    @Transactional
    fun revokeSessionFromGroup(ownerId: Int, sessionId: Int, groupId: Int) {
        log.info("Attempting to revoke access to Session ID: $sessionId from Group ID: $groupId by Owner ID: $ownerId")

        if (!sessionRepository.existsByIdAndOwnerId(sessionId, ownerId)) {
            log.warn("Revoke rejected: User ID $ownerId is not the owner of Session ID $sessionId")
            throw UnauthorizedException("You do not have permission to revoke access to this session")
        }

        // Single DB command: DELETE WHERE groupId = ? AND sessionId = ?
        groupSessionAccessRepository.deleteByGroupIdAndSessionId(groupId, sessionId)
        log.info("Successfully revoked access to Session ID: $sessionId for Group ID: $groupId")
    }
}