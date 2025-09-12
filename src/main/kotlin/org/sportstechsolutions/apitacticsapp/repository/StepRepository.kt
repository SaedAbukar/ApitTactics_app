package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.Step
import org.sportstechsolutions.apitacticsapp.model.Session
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StepRepository : JpaRepository<Step, Int> {
    fun findBySession(session: Session): List<Step>
}
