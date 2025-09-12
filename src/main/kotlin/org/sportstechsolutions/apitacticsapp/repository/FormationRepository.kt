package org.sportstechsolutions.apitacticsapp.repository
import org.sportstechsolutions.apitacticsapp.model.Formation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FormationRepository : JpaRepository<Formation, Int>