package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.GroupSessionAccess
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GroupSessionAccessRepository : JpaRepository<GroupSessionAccess, Int> {
    fun findBySessionIdAndGroupId(sessionId: Int, groupId: Int): GroupSessionAccess?
    fun findBySessionId(sessionId: Int): List<GroupSessionAccess>

    @Query("SELECT gsa FROM GroupSessionAccess gsa JOIN gsa.group.members m WHERE m.id = :userId")
    fun findByGroupMemberId(@Param("userId") userId: Int): List<GroupSessionAccess>
}
