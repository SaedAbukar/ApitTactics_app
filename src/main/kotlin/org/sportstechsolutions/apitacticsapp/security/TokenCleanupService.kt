package org.sportstechsolutions.apitacticsapp.security

import org.slf4j.LoggerFactory
import org.sportstechsolutions.apitacticsapp.repository.OAuth2ExchangeCodeRepository
import org.sportstechsolutions.apitacticsapp.repository.RefreshTokenRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class TokenCleanupService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val exchangeCodeRepository: OAuth2ExchangeCodeRepository
) {
    private val log = LoggerFactory.getLogger(TokenCleanupService::class.java)

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    fun cleanUpExpiredTokens() {
        val now = Instant.now()
        log.info("Starting scheduled cleanup of expired tokens and exchange codes...")

        val deletedRefreshTokens = refreshTokenRepository.deleteAllExpiredSince(now)
        val deletedExchangeCodes = exchangeCodeRepository.deleteAllExpiredSince(now)

        log.info("Cleanup complete. Removed $deletedRefreshTokens refresh token(s) and $deletedExchangeCodes exchange code(s).")
    }
}