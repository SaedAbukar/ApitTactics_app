package org.sportstechsolutions.apitacticsapp.service

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

    @Transactional(readOnly = true)
    fun getUserById(userId: Int): User =
        userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found") }

    @Transactional(readOnly = true)
    fun getUserWithGroupsById(userId: Int): User =
        userRepository.findWithGroupsById(userId)
            ?: throw ResourceNotFoundException("User not found")

    @Transactional
    fun togglePublicStatus(userId: Int, isPublic: Boolean): User {
        // Fast DB-level check to ensure user exists
        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException("User not found")
        }

        // Execute the highly optimized raw SQL update
        userRepository.updatePublicStatus(userId, isPublic)

        // Because we added clearAutomatically = true to the repo,
        // fetching it again here is 100% safe and will return the fresh data!
        return userRepository.findById(userId).get()
    }

    @Transactional(readOnly = true)
    fun searchPublicUsers(query: String, page: Int, size: Int, currentUserId: Int): Slice<User> {
        val pageable = PageRequest.of(page, size)

        return userRepository.findByEmailStartingWithIgnoreCaseAndIsPublicTrueAndIdNot(
            query,
            currentUserId,
            pageable
        )
    }
}