package org.sportstechsolutions.apitacticsapp.security

import org.slf4j.LoggerFactory
import org.sportstechsolutions.apitacticsapp.exception.ConflictException
import org.sportstechsolutions.apitacticsapp.exception.UnauthenticatedException
import org.sportstechsolutions.apitacticsapp.model.AuthProvider
import org.sportstechsolutions.apitacticsapp.model.RefreshToken
import org.sportstechsolutions.apitacticsapp.model.User
import org.sportstechsolutions.apitacticsapp.repository.OAuth2ExchangeCodeRepository
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
    private val refreshTokenRepository: RefreshTokenRepository,
    private val exchangeCodeRepository: OAuth2ExchangeCodeRepository
) {
    private val log = LoggerFactory.getLogger(AuthService::class.java)

    data class TokenPair(val accessToken: String, val refreshToken: String)

    @Transactional
    fun register(name: String, email: String, password: String): User {
        val normalizedEmail = email.trim().lowercase()
        val trimmedName = name.trim() // Clean up trailing spaces
        log.info("Attempting to register new local user with email: $normalizedEmail and name: $trimmedName")

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration failed: Email $normalizedEmail already exists.")
            throw ConflictException("A user with that email already exists.")
        }

        val user = userRepository.save(
            User(
                name = trimmedName, // ---> SAVED NAME HERE <---
                email = normalizedEmail,
                hashedPassword = hashEncoder.encode(password),
                authProvider = AuthProvider.LOCAL
            )
        )
        log.info("Successfully registered local user with ID: ${user.id}")
        return user
    }

    @Transactional
    fun login(email: String, password: String): TokenPair {
        val normalizedEmail = email.trim().lowercase()
        log.info("Local login attempt for email: $normalizedEmail")

        val user = userRepository.findByEmail(normalizedEmail)
            ?: throw UnauthenticatedException("Invalid credentials.")

        if (user.authProvider != AuthProvider.LOCAL) {
            log.warn("Login failed: User ${user.id} attempted local login but account is linked to ${user.authProvider}")
            throw ConflictException("This account was created via ${user.authProvider}. Please log in using that method.")
        }

        val storedPassword = user.hashedPassword
        if (storedPassword == null || !hashEncoder.matches(password, storedPassword)) {
            log.warn("Login failed: Incorrect password for User ID: ${user.id}")
            throw UnauthenticatedException("Invalid credentials.")
        }

        user.lastLogin = Instant.now()

        val newAccessToken = jwtService.generateAccessToken(user.id.toString())
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toString())

        storeRefreshToken(user.id, newRefreshToken)

        log.info("User ID: ${user.id} logged in successfully via local auth.")
        return TokenPair(newAccessToken, newRefreshToken)
    }

    @Transactional
    fun exchangeOAuth2Code(code: String): TokenPair {
        log.info("Attempting to exchange OAuth2 code for JWT tokens")

        val exchangeEntity = exchangeCodeRepository.findById(code)
            .orElseThrow {
                log.warn("Exchange failed: Invalid or missing code.")
                UnauthenticatedException("Invalid or expired exchange code.")
            }

        // Single-use security: delete immediately upon discovery
        exchangeCodeRepository.deleteById(code)

        if (exchangeEntity.expiresAt.isBefore(Instant.now())) {
            log.warn("Exchange failed: Code expired for User ID: ${exchangeEntity.userId}")
            throw UnauthenticatedException("Exchange code has expired.")
        }

        val userIdStr = exchangeEntity.userId.toString()
        val accessToken = jwtService.generateAccessToken(userIdStr)
        val refreshToken = jwtService.generateRefreshToken(userIdStr)

        storeRefreshToken(exchangeEntity.userId, refreshToken)

        log.info("Successfully exchanged OAuth2 code for tokens for User ID: ${exchangeEntity.userId}")
        return TokenPair(accessToken, refreshToken)
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

        val deletedRows = refreshTokenRepository.deleteByUserIdAndHashedTokenAtomic(userId, hashed)
        if (deletedRows == 0) {
            log.warn("Token refresh failed: Token not found in database for User ID: $userId")
            throw UnauthenticatedException("Refresh token not recognized.")
        }

        if (!userRepository.existsById(userId)) {
            log.error("Token refresh failed: User ID $userId no longer exists in the database!")
            throw UnauthenticatedException("User account no longer exists.")
        }

        val newAccessToken = jwtService.generateAccessToken(userId.toString())
        val newRefreshToken = jwtService.generateRefreshToken(userId.toString())

        storeRefreshToken(userId, newRefreshToken)

        log.info("Successfully refreshed tokens for User ID: $userId")
        return TokenPair(newAccessToken, newRefreshToken)
    }

    @Transactional
    fun logout(refreshToken: String) {
        log.info("Attempting to logout and invalidate refresh token.")
        val hashed = hashToken(refreshToken)
        val deletedCount = refreshTokenRepository.deleteByHashedToken(hashed)
        log.debug("Logout complete. Invalidated $deletedCount token(s).")
    }

    fun storeRefreshToken(userId: Int, rawRefreshToken: String) {
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
        val hashBytes = digest.digest(token.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}