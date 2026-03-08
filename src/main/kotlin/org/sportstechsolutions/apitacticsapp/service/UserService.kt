package org.sportstechsolutions.apitacticsapp.service

import org.slf4j.LoggerFactory
import org.sportstechsolutions.apitacticsapp.model.User
import org.sportstechsolutions.apitacticsapp.repository.UserRepository
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

    @Transactional(readOnly = true)
    fun getUserById(userId: Int): User {
        log.debug("Fetching user by ID: $userId")
        return userRepository.findById(userId)
            .orElseThrow {
                log.warn("User fetch failed: User $userId not found.")
                ResourceNotFoundException("User not found")
            }
    }

    @Transactional(readOnly = true)
    fun getUserWithGroupsById(userId: Int): User {
        log.debug("Fetching user with groups by ID: $userId")
        return userRepository.findWithGroupsById(userId)
            ?: throw ResourceNotFoundException("User not found")
    }

    @Transactional
    fun togglePublicStatus(userId: Int, isPublic: Boolean): User {
        log.info("Toggling public status for user: $userId to $isPublic")

        if (!userRepository.existsById(userId)) {
            log.warn("Failed to toggle public status: User $userId not found.")
            throw ResourceNotFoundException("User not found")
        }

        userRepository.updatePublicStatus(userId, isPublic)
        log.info("Successfully updated public status for user: $userId")

        return userRepository.findById(userId).get()
    }

    @Transactional(readOnly = true)
    fun searchPublicUsers(query: String, page: Int, size: Int, currentUserId: Int): Slice<User> {
        log.info("Searching public users with query: '$query', page: $page, size: $size")
        val pageable = PageRequest.of(page, size)

        return userRepository.findByEmailStartingWithIgnoreCaseAndIsPublicTrueAndIdNot(
            query, currentUserId, pageable
        )
    }
}