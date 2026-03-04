package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.GroupPracticeAccess
import org.sportstechsolutions.apitacticsapp.model.AccessRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GroupPracticeAccessRepository : JpaRepository<GroupPracticeAccess, Int> {

    fun findByPracticeIdAndGroupId(practiceId: Int, groupId: Int): GroupPracticeAccess?
    fun findByPracticeId(practiceId: Int): List<GroupPracticeAccess>

    @Query("SELECT gpa FROM GroupPracticeAccess gpa JOIN gpa.group.members m WHERE m.id = :userId")
    fun findByGroupMemberId(@Param("userId") userId: Int): List<GroupPracticeAccess>

    @Query("SELECT gpa FROM GroupPracticeAccess gpa JOIN gpa.group.members m WHERE m.id = :userId")
    fun findByGroupMemberId(@Param("userId") userId: Int, pageable: Pageable): Page<GroupPracticeAccess>

    // Checks group access in the DB without loading all members into RAM
    @Query("""
        SELECT gpa.role 
        FROM GroupPracticeAccess gpa 
        JOIN gpa.group g 
        JOIN g.members m 
        WHERE gpa.practice.id = :practiceId AND m.id = :userId
    """)
    fun findRoleForUserInPractice(@Param("userId") userId: Int, @Param("practiceId") practiceId: Int): AccessRole?

    // Bulk fetch roles to prevent N+1 queries in Search
    @Query("""
        SELECT gpa.practice.id AS practiceId, gpa.role AS role 
        FROM GroupPracticeAccess gpa 
        JOIN gpa.group g 
        JOIN g.members m 
        WHERE m.id = :userId AND gpa.practice.id IN :practiceIds
    """)
    fun findRolesForUserInPractices(@Param("userId") userId: Int, @Param("practiceIds") practiceIds: List<Int>): List<PracticeRoleProjection>

    @Query("SELECT gpa FROM GroupPracticeAccess gpa JOIN FETCH gpa.group WHERE gpa.practice.id = :practiceId")
    fun findAllWithGroupByPracticeId(@Param("practiceId") practiceId: Int): List<GroupPracticeAccess>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM GroupPracticeAccess gpa WHERE gpa.group.id = :groupId AND gpa.practice.id = :practiceId")
    fun deleteByGroupIdAndPracticeId(@Param("groupId") groupId: Int, @Param("practiceId") practiceId: Int)
}