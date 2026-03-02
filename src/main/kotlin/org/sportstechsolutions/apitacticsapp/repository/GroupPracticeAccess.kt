package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.GroupPracticeAccess
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GroupPracticeAccessRepository : JpaRepository<GroupPracticeAccess, Int> {

    fun findByPracticeIdAndGroupId(practiceId: Int, groupId: Int): GroupPracticeAccess?

    fun findByPracticeId(practiceId: Int): List<GroupPracticeAccess>

    // Standard List version
    @Query("SELECT gpa FROM GroupPracticeAccess gpa JOIN gpa.group.members m WHERE m.id = :userId")
    fun findByGroupMemberId(@Param("userId") userId: Int): List<GroupPracticeAccess>

    // Paginated version (Used in the Tabs)
    @Query("SELECT gpa FROM GroupPracticeAccess gpa JOIN gpa.group.members m WHERE m.id = :userId")
    fun findByGroupMemberId(@Param("userId") userId: Int, pageable: Pageable): Page<GroupPracticeAccess>
}