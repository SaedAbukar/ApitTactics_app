package org.sportstechsolutions.apitacticsapp.security

import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(AuthService::class.java)

    data class TokenPair(val accessToken: String, val refreshToken: String)

    @Transactional
    fun register(email: String, password: String): User {
        val trimmedEmail = email.trim()
        log.info("Attempting to register new user with email: $trimmedEmail")

        if (userRepository.existsByEmail(trimmedEmail)) {
            log.warn("Registration failed: Email $trimmedEmail already exists.")
            throw ConflictException("A user with that email already exists.")
        }

        val user = userRepository.save(User(email = trimmedEmail, hashedPassword = hashEncoder.encode(password)))
        log.info("Successfully registered user with ID: ${user.id}")
        return user
    }

    @Transactional
    fun login(email: String, password: String): TokenPair {
        val trimmedEmail = email.trim()
        log.info("Login attempt for email: $trimmedEmail")

        val user = userRepository.findByEmail(trimmedEmail)
            ?: throw UnauthenticatedException("Invalid credentials.")

        if (!hashEncoder.matches(password, user.hashedPassword)) {
            log.warn("Login failed for user ID: ${user.id} - Incorrect password.")
            throw UnauthenticatedException("Invalid credentials.")
        }

        user.lastLogin = Instant.now()

        val newAccessToken = jwtService.generateAccessToken(user.id.toString())
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toString())

        storeRefreshToken(user.id, newRefreshToken)

        log.info("User ID: ${user.id} logged in successfully.")
        return TokenPair(accessToken = newAccessToken, refreshToken = newRefreshToken)
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        log.info("Attempting to refresh tokens.")

        if (!jwtService.validateRefreshToken(refreshToken)) {
            log.warn("Token refresh failed: Invalid JWT signature or expiration.")
            throw UnauthenticatedException("Invalid refresh token.")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val hashed = hashToken(refreshToken)

        if (!refreshTokenRepository.existsByUserIdAndHashedToken(userId, hashed)) {
            log.warn("Token refresh failed: Token not found in database for User ID: $userId")
            throw UnauthenticatedException("Refresh token not recognized (maybe used or expired?)")
        }

        refreshTokenRepository.deleteByUserIdAndHashedToken(userId, hashed)

        if (!userRepository.existsById(userId)) {
            log.error("Token refresh failed: User ID: $userId no longer exists in database!")
            throw UnauthenticatedException("User account no longer exists.")
        }

        val newAccessToken = jwtService.generateAccessToken(userId.toString())
        val newRefreshToken = jwtService.generateRefreshToken(userId.toString())

        storeRefreshToken(userId, newRefreshToken)

        log.info("Successfully refreshed tokens for User ID: $userId")
        return TokenPair(accessToken = newAccessToken, refreshToken = newRefreshToken)
    }

    private fun storeRefreshToken(userId: Int, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshToken(userId = userId, expiresAt = expiresAt, hashedToken = hashed)
        )
        log.debug("Stored new refresh token in database for User ID: $userId")
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}