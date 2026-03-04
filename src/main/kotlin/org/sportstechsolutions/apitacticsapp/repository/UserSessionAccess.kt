package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.Session
import org.sportstechsolutions.apitacticsapp.model.UserSessionAccess
import org.sportstechsolutions.apitacticsapp.model.AccessRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserSessionAccessRepository : JpaRepository<UserSessionAccess, Int> {

    // DB-level filtering (Used in the Tabs to fix pagination math)
    fun findByUserIdAndRoleNot(userId: Int, role: AccessRole, pageable: Pageable): Page<UserSessionAccess>

    fun findByUserId(userId: Int): List<UserSessionAccess>
    fun findByUserIdAndSessionId(userId: Int, sessionId: Int): UserSessionAccess?
    fun findBySessionId(sessionId: Int): List<UserSessionAccess>
    fun deleteAllBySession(session: Session)

    // Bulk fetch roles to prevent N+1 queries in Search
    @Query("SELECT usa.session.id AS sessionId, usa.role AS role FROM UserSessionAccess usa WHERE usa.user.id = :userId AND usa.session.id IN :sessionIds")
    fun findRolesForUserInSessions(@Param("userId") userId: Int, @Param("sessionIds") sessionIds: List<Int>): List<SessionRoleProjection>

    // Fetches the access record AND the user in exactly 1 query (Fixes N+1)
    @Query("SELECT usa FROM UserSessionAccess usa JOIN FETCH usa.user WHERE usa.session.id = :sessionId")
    fun findAllWithUserBySessionId(@Param("sessionId") sessionId: Int): List<UserSessionAccess>

    // Deletes the record directly in SQL without loading it into Kotlin memory
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserSessionAccess usa WHERE usa.user.id = :userId AND usa.session.id = :sessionId")
    fun deleteByUserIdAndSessionId(@Param("userId") userId: Int, @Param("sessionId") sessionId: Int)
}