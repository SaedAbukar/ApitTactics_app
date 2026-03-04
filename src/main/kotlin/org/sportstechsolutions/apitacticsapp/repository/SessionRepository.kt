package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.Session
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

// --- PROJECTION FOR BULK ROLE FETCHING ---
interface SessionRoleProjection {
    val sessionId: Int
    val role: org.sportstechsolutions.apitacticsapp.model.AccessRole
}

@Repository
interface SessionRepository : JpaRepository<Session, Int>, JpaSpecificationExecutor<Session> {

    fun findByOwnerId(ownerId: Int, pageable: Pageable): Page<Session>

    // Note: findAllAccessibleSessionIds was completely REMOVED.
    // This logic is now safely handled by EXISTS subqueries in SearchSpecifications.

    // --- FAVORITES OPTIMIZATION ---
    @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END 
        FROM Session s JOIN s.favoritedByUsers u 
        WHERE s.id = :sessionId AND u.id = :userId
    """)
    fun isSessionFavoritedByUser(@Param("sessionId") sessionId: Int, @Param("userId") userId: Int): Boolean

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT INTO user_favorite_session (session_id, user_id) VALUES (:sessionId, :userId)", nativeQuery = true)
    fun addFavorite(@Param("sessionId") sessionId: Int, @Param("userId") userId: Int)

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM user_favorite_session WHERE session_id = :sessionId AND user_id = :userId", nativeQuery = true)
    fun removeFavorite(@Param("sessionId") sessionId: Int, @Param("userId") userId: Int)

    fun existsByIdAndOwnerId(id: Int, ownerId: Int): Boolean
}