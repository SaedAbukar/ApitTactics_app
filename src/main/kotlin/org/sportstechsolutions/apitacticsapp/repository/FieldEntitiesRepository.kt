package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlayerRepository : JpaRepository<Player, Int>
@Repository
interface BallRepository : JpaRepository<Ball, Int>
@Repository
interface GoalRepository : JpaRepository<Goal, Int>
@Repository
interface ConeRepository : JpaRepository<Cone, Int>
@Repository
interface FormationPositionRepository : JpaRepository<FormationPosition, Int>
