package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.GroupSessionAccess
import org.sportstechsolutions.apitacticsapp.model.AccessRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GroupSessionAccessRepository : JpaRepository<GroupSessionAccess, Int> {

    fun findBySessionIdAndGroupId(sessionId: Int, groupId: Int): GroupSessionAccess?
    fun findBySessionId(sessionId: Int): List<GroupSessionAccess>

    @Query("SELECT gsa FROM GroupSessionAccess gsa JOIN gsa.group.members m WHERE m.id = :userId")
    fun findByGroupMemberId(@Param("userId") userId: Int): List<GroupSessionAccess>

    @Query("SELECT gsa FROM GroupSessionAccess gsa JOIN gsa.group.members m WHERE m.id = :userId")
    fun findByGroupMemberId(@Param("userId") userId: Int, pageable: Pageable): Page<GroupSessionAccess>

    // Checks group access in the DB without loading all members into application memory
    @Query("""
        SELECT gsa.role 
        FROM GroupSessionAccess gsa 
        JOIN gsa.group g 
        JOIN g.members m 
        WHERE gsa.session.id = :sessionId AND m.id = :userId
    """)
    fun findRoleForUserInSession(@Param("userId") userId: Int, @Param("sessionId") sessionId: Int): AccessRole?

    // Bulk fetch roles to prevent N+1 queries in Search
    @Query("""
        SELECT gsa.session.id AS sessionId, gsa.role AS role 
        FROM GroupSessionAccess gsa 
        JOIN gsa.group g 
        JOIN g.members m 
        WHERE m.id = :userId AND gsa.session.id IN :sessionIds
    """)
    fun findRolesForUserInSessions(@Param("userId") userId: Int, @Param("sessionIds") sessionIds: List<Int>): List<SessionRoleProjection>

    // Fetches the access record AND the group in exactly 1 query (Fixes N+1)
    @Query("SELECT gsa FROM GroupSessionAccess gsa JOIN FETCH gsa.group WHERE gsa.session.id = :sessionId")
    fun findAllWithGroupBySessionId(@Param("sessionId") sessionId: Int): List<GroupSessionAccess>

    // Deletes the record directly in SQL without loading it into Kotlin memory
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM GroupSessionAccess gsa WHERE gsa.group.id = :groupId AND gsa.session.id = :sessionId")
    fun deleteByGroupIdAndSessionId(@Param("groupId") groupId: Int, @Param("sessionId") sessionId: Int)
}