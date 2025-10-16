package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.GroupPracticeAccess
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GroupPracticeAccessRepository : JpaRepository<GroupPracticeAccess, Int> {
    fun findByPracticeIdAndGroupId(practiceId: Int, groupId: Int): GroupPracticeAccess?
    fun findByPracticeId(practiceId: Int): List<GroupPracticeAccess>

    @Query("SELECT gpa FROM GroupPracticeAccess gpa JOIN gpa.group.members m WHERE m.id = :userId")
    fun findByGroupMemberId(@Param("userId") userId: Int): List<GroupPracticeAccess>
}
