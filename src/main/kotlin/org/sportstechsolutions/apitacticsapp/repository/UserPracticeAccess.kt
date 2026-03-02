package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.Practice
import org.sportstechsolutions.apitacticsapp.model.UserPracticeAccess
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface UserPracticeAccessRepository : JpaRepository<UserPracticeAccess, Int> {

    // Paginated version (Used in the Tabs)
    fun findByUserId(userId: Int, pageable: Pageable): Page<UserPracticeAccess>

    // Standard List version
    fun findByUserId(userId: Int): List<UserPracticeAccess>

    fun findByUserIdAndPracticeId(userId: Int, practiceId: Int): UserPracticeAccess?
    fun findByPracticeId(practiceId: Int): List<UserPracticeAccess>
    fun deleteAllByPractice(practice: Practice)
}