package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.OAuth2ExchangeCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface OAuth2ExchangeCodeRepository : JpaRepository<OAuth2ExchangeCode, String> {
    @Modifying
    @Query("DELETE FROM OAuth2ExchangeCode e WHERE e.expiresAt < :now")
    fun deleteAllExpiredSince(now: Instant): Int
}