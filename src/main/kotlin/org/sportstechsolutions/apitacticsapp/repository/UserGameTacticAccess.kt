package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.GameTactic
import org.sportstechsolutions.apitacticsapp.model.UserGameTacticAccess
import org.sportstechsolutions.apitacticsapp.model.AccessRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserGameTacticAccessRepository : JpaRepository<UserGameTacticAccess, Int> {

    // DB-level filtering (Used in the Tabs to fix pagination math)
    fun findByUserIdAndRoleNot(userId: Int, role: AccessRole, pageable: Pageable): Page<UserGameTacticAccess>

    fun findByUserId(userId: Int): List<UserGameTacticAccess>
    fun findByUserIdAndGameTacticId(userId: Int, tacticId: Int): UserGameTacticAccess?
    fun findByGameTacticId(tacticId: Int): List<UserGameTacticAccess>
    fun deleteAllByGameTactic(gameTactic: GameTactic)

    // Bulk fetch roles to prevent N+1 queries in Search
    @Query("SELECT uga.gameTactic.id AS gameTacticId, uga.role AS role FROM UserGameTacticAccess uga WHERE uga.user.id = :userId AND uga.gameTactic.id IN :tacticIds")
    fun findRolesForUserInGameTactics(@Param("userId") userId: Int, @Param("tacticIds") tacticIds: List<Int>): List<GameTacticRoleProjection>

    @Query("SELECT uga FROM UserGameTacticAccess uga JOIN FETCH uga.user WHERE uga.gameTactic.id = :tacticId")
    fun findAllWithUserByGameTacticId(@Param("tacticId") tacticId: Int): List<UserGameTacticAccess>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserGameTacticAccess uga WHERE uga.user.id = :userId AND uga.gameTactic.id = :tacticId")
    fun deleteByUserIdAndGameTacticId(@Param("userId") userId: Int, @Param("tacticId") tacticId: Int)
}