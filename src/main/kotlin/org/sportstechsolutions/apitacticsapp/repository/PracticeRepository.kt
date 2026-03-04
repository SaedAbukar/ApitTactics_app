package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.Practice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

// --- PROJECTION FOR BULK ROLE FETCHING ---
interface PracticeRoleProjection {
    val practiceId: Int
    val role: org.sportstechsolutions.apitacticsapp.model.AccessRole
}

@Repository
interface PracticeRepository : JpaRepository<Practice, Int>, JpaSpecificationExecutor<Practice> {

    fun findByOwnerId(ownerId: Int, pageable: Pageable): Page<Practice>


    // --- FAVORITES OPTIMIZATION ---
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END 
        FROM Practice p JOIN p.favoritedByUsers u 
        WHERE p.id = :practiceId AND u.id = :userId
    """)
    fun isPracticeFavoritedByUser(@Param("practiceId") practiceId: Int, @Param("userId") userId: Int): Boolean

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT INTO user_favorite_practice (practice_id, user_id) VALUES (:practiceId, :userId)", nativeQuery = true)
    fun addFavorite(@Param("practiceId") practiceId: Int, @Param("userId") userId: Int)

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM user_favorite_practice WHERE practice_id = :practiceId AND user_id = :userId", nativeQuery = true)
    fun removeFavorite(@Param("practiceId") practiceId: Int, @Param("userId") userId: Int)

    // In PracticeRepository
    fun existsByIdAndOwnerId(id: Int, ownerId: Int): Boolean
}