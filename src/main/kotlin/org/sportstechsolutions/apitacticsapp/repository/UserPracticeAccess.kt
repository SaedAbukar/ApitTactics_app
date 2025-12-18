package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.Practice
import org.sportstechsolutions.apitacticsapp.model.UserPracticeAccess
import org.springframework.data.jpa.repository.JpaRepository

interface UserPracticeAccessRepository : JpaRepository<UserPracticeAccess, Int> {
    fun findByUserId(userId: Int): List<UserPracticeAccess>
    fun findByUserIdAndPracticeId(userId: Int, practiceId: Int): UserPracticeAccess?

    fun deleteAllByPractice(practice: Practice)

}
