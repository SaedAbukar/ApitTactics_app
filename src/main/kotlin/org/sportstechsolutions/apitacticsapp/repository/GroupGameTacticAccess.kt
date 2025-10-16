package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.GroupGameTacticAccess
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GroupGameTacticAccessRepository : JpaRepository<GroupGameTacticAccess, Int> {
    fun findByGameTacticIdAndGroupId(tacticId: Int, groupId: Int): GroupGameTacticAccess?
    fun findByGameTacticId(tacticId: Int): List<GroupGameTacticAccess>

    @Query("SELECT gga FROM GroupGameTacticAccess gga JOIN gga.group.members m WHERE m.id = :userId")
    fun findByGroupMemberId(@Param("userId") userId: Int): List<GroupGameTacticAccess>
}
