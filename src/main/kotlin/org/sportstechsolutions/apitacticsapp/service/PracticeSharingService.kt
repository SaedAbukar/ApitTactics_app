package org.sportstechsolutions.apitacticsapp.service

import org.slf4j.LoggerFactory
import org.sportstechsolutions.apitacticsapp.dtos.CollaboratorDTO
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

    private val log = LoggerFactory.getLogger(PracticeSharingService::class.java)

    @Transactional(readOnly = true)
    fun getPracticeCollaborators(ownerId: Int, practiceId: Int): List<CollaboratorDTO> {
        log.debug("Fetching collaborators for Practice ID: $practiceId requested by User ID: $ownerId")

        // Fast ownership check without loading the entire Practice entity into memory
        if (!practiceRepository.existsByIdAndOwnerId(practiceId, ownerId)) {
            log.warn("Access denied: User ID $ownerId is not the owner of Practice ID $practiceId")
            throw UnauthorizedException("Only the owner can view collaborators or practice does not exist")
        }

        val collaborators = mutableListOf<CollaboratorDTO>()

        // Uses JOIN FETCH to get all users in 1 query (fixes N+1)
        val userAccess = userPracticeAccessRepository.findAllWithUserByPracticeId(practiceId)
            .mapNotNull { access ->
                val user = access.user ?: return@mapNotNull null
                CollaboratorDTO(user.id, user.email, CollaboratorType.USER, access.role)
            }

        // Uses JOIN FETCH to get all groups in 1 query (fixes N+1)
        val groupAccess = groupPracticeAccessRepository.findAllWithGroupByPracticeId(practiceId)
            .mapNotNull { access ->
                val group = access.group ?: return@mapNotNull null
                CollaboratorDTO(group.id, group.name, CollaboratorType.GROUP, access.role)
            }

        collaborators.addAll(userAccess)
        collaborators.addAll(groupAccess)

        log.debug("Successfully retrieved ${collaborators.size} collaborators for Practice ID: $practiceId")
        return collaborators
    }

    @Transactional
    fun sharePracticeWithUser(ownerId: Int, practiceId: Int, targetUserId: Int, role: AccessRole) {
        log.info("Attempting to share Practice ID: $practiceId with User ID: $targetUserId by Owner ID: $ownerId")

        if (ownerId == targetUserId) {
            log.warn("Share rejected: User ID $ownerId attempted to share Practice ID $practiceId with themselves")
            throw IllegalArgumentException("Cannot share with yourself")
        }

        if (!practiceRepository.existsByIdAndOwnerId(practiceId, ownerId)) {
            log.warn("Share rejected: User ID $ownerId is not the owner of Practice ID $practiceId")
            throw UnauthorizedException("You do not have permission to share this practice")
        }

        val existingAccess = userPracticeAccessRepository.findByUserIdAndPracticeId(targetUserId, practiceId)
        if (existingAccess != null) {
            log.debug("Updating existing access role for User ID $targetUserId on Practice ID $practiceId to $role")
            existingAccess.role = role
            // Hibernate auto-updates managed entities on transaction commit; no .save() needed
        } else {
            log.debug("Creating new access grant for User ID $targetUserId on Practice ID $practiceId with role $role")
            // getReferenceById creates a Proxy. No SELECT queries are executed!
            val practiceProxy = practiceRepository.getReferenceById(practiceId)
            val userProxy = userRepository.getReferenceById(targetUserId)
            userPracticeAccessRepository.save(UserPracticeAccess(user = userProxy, practice = practiceProxy, role = role))
        }
        log.info("Successfully shared Practice ID: $practiceId with User ID: $targetUserId")
    }

    @Transactional
    fun revokePracticeFromUser(ownerId: Int, practiceId: Int, targetUserId: Int) {
        log.info("Attempting to revoke access to Practice ID: $practiceId from User ID: $targetUserId by Owner ID: $ownerId")

        if (!practiceRepository.existsByIdAndOwnerId(practiceId, ownerId)) {
            log.warn("Revoke rejected: User ID $ownerId is not the owner of Practice ID $practiceId")
            throw UnauthorizedException("You do not have permission to revoke access to this practice")
        }

        // Single DB command: DELETE WHERE userId = ? AND practiceId = ?
        userPracticeAccessRepository.deleteByUserIdAndPracticeId(targetUserId, practiceId)
        log.info("Successfully revoked access to Practice ID: $practiceId for User ID: $targetUserId")
    }

    @Transactional
    fun sharePracticeWithGroup(ownerId: Int, practiceId: Int, groupId: Int, role: AccessRole) {
        log.info("Attempting to share Practice ID: $practiceId with Group ID: $groupId by Owner ID: $ownerId")

        if (!practiceRepository.existsByIdAndOwnerId(practiceId, ownerId)) {
            log.warn("Share rejected: User ID $ownerId is not the owner of Practice ID $practiceId")
            throw UnauthorizedException("You do not have permission to share this practice")
        }

        val existingAccess = groupPracticeAccessRepository.findByPracticeIdAndGroupId(practiceId, groupId)
        if (existingAccess != null) {
            log.debug("Updating existing access role for Group ID $groupId on Practice ID $practiceId to $role")
            existingAccess.role = role
        } else {
            log.debug("Creating new access grant for Group ID $groupId on Practice ID $practiceId with role $role")
            // getReferenceById creates a Proxy. No SELECT queries are executed!
            val practiceProxy = practiceRepository.getReferenceById(practiceId)
            val groupProxy = groupRepository.getReferenceById(groupId)
            groupPracticeAccessRepository.save(GroupPracticeAccess(practice = practiceProxy, group = groupProxy, role = role))
        }
        log.info("Successfully shared Practice ID: $practiceId with Group ID: $groupId")
    }

    @Transactional
    fun revokePracticeFromGroup(ownerId: Int, practiceId: Int, groupId: Int) {
        log.info("Attempting to revoke access to Practice ID: $practiceId from Group ID: $groupId by Owner ID: $ownerId")

        if (!practiceRepository.existsByIdAndOwnerId(practiceId, ownerId)) {
            log.warn("Revoke rejected: User ID $ownerId is not the owner of Practice ID $practiceId")
            throw UnauthorizedException("You do not have permission to revoke access to this practice")
        }

        // Single DB command: DELETE WHERE groupId = ? AND practiceId = ?
        groupPracticeAccessRepository.deleteByGroupIdAndPracticeId(groupId, practiceId)
        log.info("Successfully revoked access to Practice ID: $practiceId for Group ID: $groupId")
    }
}