package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.User
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<User, Int> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean


    fun findByEmailStartingWithIgnoreCaseAndIsPublicTrueAndIdNot(
        query: String,
        excludeId: Int,
        pageable: Pageable
    ): Slice<User>


    @Modifying
    @Query("UPDATE User u SET u.isPublic = :isPublic WHERE u.id = :id")
    fun updatePublicStatus(id: Int, isPublic: Boolean)
    @EntityGraph(attributePaths = ["groups"])
    fun findWithGroupsById(id: Int): User?
}
