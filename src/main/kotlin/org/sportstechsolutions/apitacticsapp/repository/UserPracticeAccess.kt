package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.Practice
import org.sportstechsolutions.apitacticsapp.model.UserPracticeAccess
import org.sportstechsolutions.apitacticsapp.model.AccessRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserPracticeAccessRepository : JpaRepository<UserPracticeAccess, Int> {

    // DB-level filtering (Fixes pagination math)
    fun findByUserIdAndRoleNot(userId: Int, role: AccessRole, pageable: Pageable): Page<UserPracticeAccess>

    fun findByUserId(userId: Int): List<UserPracticeAccess>
    fun findByUserIdAndPracticeId(userId: Int, practiceId: Int): UserPracticeAccess?
    fun findByPracticeId(practiceId: Int): List<UserPracticeAccess>
    fun deleteAllByPractice(practice: Practice)

    // Bulk fetch roles to prevent N+1 queries in Search
    @Query("SELECT upa.practice.id AS practiceId, upa.role AS role FROM UserPracticeAccess upa WHERE upa.user.id = :userId AND upa.practice.id IN :practiceIds")
    fun findRolesForUserInPractices(@Param("userId") userId: Int, @Param("practiceIds") practiceIds: List<Int>): List<PracticeRoleProjection>

    @Query("SELECT upa FROM UserPracticeAccess upa JOIN FETCH upa.user WHERE upa.practice.id = :practiceId")
    fun findAllWithUserByPracticeId(@Param("practiceId") practiceId: Int): List<UserPracticeAccess>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserPracticeAccess upa WHERE upa.user.id = :userId AND upa.practice.id = :practiceId")
    fun deleteByUserIdAndPracticeId(@Param("userId") userId: Int, @Param("practiceId") practiceId: Int)
}