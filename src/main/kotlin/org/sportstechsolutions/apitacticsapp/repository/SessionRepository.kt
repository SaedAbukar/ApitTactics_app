package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.Session
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SessionRepository : JpaRepository<Session, Int>, JpaSpecificationExecutor<Session> {

    fun findByOwnerId(ownerId: Int, pageable: Pageable): Page<Session>

    @Query("""
    SELECT DISTINCT s.id FROM Session s 
    LEFT JOIN s.owner o 
    LEFT JOIN s.userAccess ua 
    LEFT JOIN s.groupAccess ga 
    LEFT JOIN ga.group g 
    LEFT JOIN g.members m 
    WHERE o.id = :userId 
       OR ua.user.id = :userId 
       OR m.id = :userId 
       OR s.isPremade = true
       OR s.isPublic = true
""")
    fun findAllAccessibleSessionIds(@Param("userId") userId: Int): Set<Int>
}