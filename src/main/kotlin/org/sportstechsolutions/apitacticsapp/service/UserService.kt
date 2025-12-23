package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.model.User
import org.sportstechsolutions.apitacticsapp.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class UserService(
    private val userRepository: UserRepository
) {
    fun getUserById(userId: Int): User =
        userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

    fun getUserWithGroupsById(userId: Int): User? =
        userRepository.findWithGroupsById(userId)

    @Transactional
    fun togglePublicStatus(userId: Int, isPublic: Boolean): User {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        user.isPublic = isPublic
        return userRepository.save(user)
    }

    fun searchPublicUsers(query: String, page: Int, size: Int, currentUserId: Int): Slice<User> {
        val pageable = PageRequest.of(page, size)

        return userRepository.findByEmailStartingWithIgnoreCaseAndIsPublicTrueAndIdNot(
            query,
            currentUserId,
            pageable
        )
    }
}
