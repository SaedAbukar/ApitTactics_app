package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.Practice
import org.springframework.data.jpa.repository.JpaRepository

interface PracticeRepository : JpaRepository<Practice, Int> {
    fun findByOwnerId(ownerId: Int): List<Practice>
}
