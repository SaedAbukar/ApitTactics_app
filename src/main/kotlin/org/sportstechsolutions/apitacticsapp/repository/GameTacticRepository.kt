package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.GameTactic
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

// --- PROJECTION FOR BULK ROLE FETCHING ---
interface GameTacticRoleProjection {
    val gameTacticId: Int
    val role: org.sportstechsolutions.apitacticsapp.model.AccessRole
}

@Repository
interface GameTacticRepository : JpaRepository<GameTactic, Int>, JpaSpecificationExecutor<GameTactic> {

    fun findByOwnerId(ownerId: Int, pageable: Pageable): Page<GameTactic>

    // --- FAVORITES OPTIMIZATION ---
    @Query("""
        SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END 
        FROM GameTactic g JOIN g.favoritedByUsers u 
        WHERE g.id = :tacticId AND u.id = :userId
    """)
    fun isGameTacticFavoritedByUser(@Param("tacticId") tacticId: Int, @Param("userId") userId: Int): Boolean

    // Note: Update 'user_favorite_game_tactic' to match your actual join table name if needed
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT INTO user_favorite_game_tactic (game_tactic_id, user_id) VALUES (:tacticId, :userId)", nativeQuery = true)
    fun addFavorite(@Param("tacticId") tacticId: Int, @Param("userId") userId: Int)

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM user_favorite_game_tactic WHERE game_tactic_id = :tacticId AND user_id = :userId", nativeQuery = true)
    fun removeFavorite(@Param("tacticId") tacticId: Int, @Param("userId") userId: Int)

    fun existsByIdAndOwnerId(id: Int, ownerId: Int): Boolean
}