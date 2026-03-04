package org.sportstechsolutions.apitacticsapp.security

import org.sportstechsolutions.apitacticsapp.exception.ConflictException
import org.sportstechsolutions.apitacticsapp.exception.UnauthenticatedException
import org.sportstechsolutions.apitacticsapp.model.RefreshToken
import org.sportstechsolutions.apitacticsapp.model.User
import org.sportstechsolutions.apitacticsapp.repository.RefreshTokenRepository
import org.sportstechsolutions.apitacticsapp.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.*

@Service
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String
    )

    @Transactional
    fun register(email: String, password: String): User {
        val trimmedEmail = email.trim()

        // Fast DB-level check (avoids pulling entity into memory)
        if (userRepository.existsByEmail(trimmedEmail)) {
            throw ConflictException("A user with that email already exists.")
        }

        return userRepository.save(
            User(
                email = trimmedEmail,
                hashedPassword = hashEncoder.encode(password)
            )
        )
    }

    @Transactional
    fun login(email: String, password: String): TokenPair {
        val user = userRepository.findByEmail(email.trim())
            ?: throw UnauthenticatedException("Invalid credentials.")

        if (!hashEncoder.matches(password, user.hashedPassword)) {
            throw UnauthenticatedException("Invalid credentials.")
        }

        // Hibernate's "Dirty Checking" will automatically issue an UPDATE statement
        // for this when the transaction commits. No need to call userRepository.save()!
        user.lastLogin = Instant.now()

        val newAccessToken = jwtService.generateAccessToken(user.id.toString())
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toString())

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw UnauthenticatedException("Invalid refresh token.")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)

        val hashed = hashToken(refreshToken)

        // Fast existence check instead of fetching the whole entity
        if (!refreshTokenRepository.existsByUserIdAndHashedToken(userId, hashed)) {
            throw UnauthenticatedException("Refresh token not recognized (maybe used or expired?)")
        }

        // Direct SQL delete
        refreshTokenRepository.deleteByUserIdAndHashedToken(userId, hashed)

        // Ensure the user wasn't deleted from the DB while holding a valid token
        if (!userRepository.existsById(userId)) {
            throw UnauthenticatedException("User account no longer exists.")
        }

        val newAccessToken = jwtService.generateAccessToken(userId.toString())
        val newRefreshToken = jwtService.generateRefreshToken(userId.toString())

        storeRefreshToken(userId, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    private fun storeRefreshToken(userId: Int, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                expiresAt = expiresAt,
                hashedToken = hashed
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}