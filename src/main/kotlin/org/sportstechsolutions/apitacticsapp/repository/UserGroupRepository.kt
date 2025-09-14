package org.sportstechsolutions.apitacticsapp.repository

import org.sportstechsolutions.apitacticsapp.model.UserGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserGroupRepository : JpaRepository<UserGroup, Int>
