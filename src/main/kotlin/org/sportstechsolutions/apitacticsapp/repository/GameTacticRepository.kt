package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.GameTactic
import org.springframework.data.jpa.repository.JpaRepository

interface GameTacticRepository : JpaRepository<GameTactic, Int> {
    fun findByOwnerId(ownerId: Int): List<GameTactic>
}
