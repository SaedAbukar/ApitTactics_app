package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.GameTactic
import org.sportstechsolutions.apitacticsapp.model.UserGameTacticAccess
import org.springframework.data.jpa.repository.JpaRepository

interface UserGameTacticAccessRepository : JpaRepository<UserGameTacticAccess, Int> {
    fun findByUserId(userId: Int): List<UserGameTacticAccess>
    fun findByUserIdAndGameTacticId(userId: Int, tacticId: Int): UserGameTacticAccess?
    fun findByGameTacticId(tacticId: Int): List<UserGameTacticAccess>
    fun deleteAllByGameTactic(gameTactic: GameTactic)

}
