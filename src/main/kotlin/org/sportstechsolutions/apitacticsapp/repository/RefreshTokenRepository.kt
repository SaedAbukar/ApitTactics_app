package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface RefreshTokenRepository: JpaRepository<RefreshToken, Long> {
    fun findByUserIdAndHashedToken(userId: Int, hashedToken: String): RefreshToken?


    // Fast DB-level check without loading the token into memory
    fun existsByUserIdAndHashedToken(userId: Int, hashedToken: String): Boolean

    // Direct, optimized SQL delete
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshToken r WHERE r.userId = :userId AND r.hashedToken = :hashedToken")
    fun deleteByUserIdAndHashedToken(@Param("userId") userId: Int, @Param("hashedToken") hashedToken: String)

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshToken r WHERE r.userId = :userId AND r.hashedToken = :hashedToken")
    fun deleteByUserIdAndHashedTokenAtomic(userId: Int, hashedToken: String): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshToken r WHERE r.hashedToken = :hashedToken")
    fun deleteByHashedToken(hashedToken: String): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    fun deleteAllExpiredSince(now: Instant): Int
}