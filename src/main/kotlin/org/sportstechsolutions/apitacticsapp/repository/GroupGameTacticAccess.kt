package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.GroupGameTacticAccess
import org.sportstechsolutions.apitacticsapp.model.AccessRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GroupGameTacticAccessRepository : JpaRepository<GroupGameTacticAccess, Int> {

    fun findByGameTacticIdAndGroupId(tacticId: Int, groupId: Int): GroupGameTacticAccess?
    fun findByGameTacticId(tacticId: Int): List<GroupGameTacticAccess>

    @Query("SELECT gga FROM GroupGameTacticAccess gga JOIN gga.group.members m WHERE m.id = :userId")
    fun findByGroupMemberId(@Param("userId") userId: Int): List<GroupGameTacticAccess>

    @Query("SELECT gga FROM GroupGameTacticAccess gga JOIN gga.group.members m WHERE m.id = :userId")
    fun findByGroupMemberId(@Param("userId") userId: Int, pageable: Pageable): Page<GroupGameTacticAccess>

    // Checks group access in the DB without loading all members into application memory
    @Query("""
        SELECT gga.role 
        FROM GroupGameTacticAccess gga 
        JOIN gga.group g 
        JOIN g.members m 
        WHERE gga.gameTactic.id = :tacticId AND m.id = :userId
    """)
    fun findRoleForUserInGameTactic(@Param("userId") userId: Int, @Param("tacticId") tacticId: Int): AccessRole?

    // Bulk fetch roles to prevent N+1 queries in Search
    @Query("""
        SELECT gga.gameTactic.id AS gameTacticId, gga.role AS role 
        FROM GroupGameTacticAccess gga 
        JOIN gga.group g 
        JOIN g.members m 
        WHERE m.id = :userId AND gga.gameTactic.id IN :tacticIds
    """)
    fun findRolesForUserInGameTactics(@Param("userId") userId: Int, @Param("tacticIds") tacticIds: List<Int>): List<GameTacticRoleProjection>

    @Query("SELECT gga FROM GroupGameTacticAccess gga JOIN FETCH gga.group WHERE gga.gameTactic.id = :tacticId")
    fun findAllWithGroupByGameTacticId(@Param("tacticId") tacticId: Int): List<GroupGameTacticAccess>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM GroupGameTacticAccess gga WHERE gga.group.id = :groupId AND gga.gameTactic.id = :tacticId")
    fun deleteByGroupIdAndGameTacticId(@Param("groupId") groupId: Int, @Param("tacticId") tacticId: Int)
}