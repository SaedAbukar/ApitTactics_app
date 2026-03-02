package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.Session
import org.sportstechsolutions.apitacticsapp.model.UserSessionAccess
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface UserSessionAccessRepository : JpaRepository<UserSessionAccess, Int> {

    // Paginated version (Used in the Tabs)
    fun findByUserId(userId: Int, pageable: Pageable): Page<UserSessionAccess>

    // Standard List version
    fun findByUserId(userId: Int): List<UserSessionAccess>

    fun findByUserIdAndSessionId(userId: Int, sessionId: Int): UserSessionAccess?
    fun findBySessionId(sessionId: Int): List<UserSessionAccess>

    // This will work fine as long as the SERVICE calling it is @Transactional
    fun deleteAllBySession(session: Session)

    // Optional: If you ever need to revoke access by IDs without loading the whole object
    fun deleteByUserIdAndSessionId(userId: Int, sessionId: Int)
}