package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.UserSessionAccess
import org.springframework.data.jpa.repository.JpaRepository

interface UserSessionAccessRepository : JpaRepository<UserSessionAccess, Int> {
    fun findByUserId(userId: Int): List<UserSessionAccess>
    fun findByUserIdAndSessionId(userId: Int, sessionId: Int): UserSessionAccess?
}
