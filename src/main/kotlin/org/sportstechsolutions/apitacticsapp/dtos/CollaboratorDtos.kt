package org.sportstechsolutions.apitacticsapp.dtos

import org.sportstechsolutions.apitacticsapp.model.AccessRole
import org.sportstechsolutions.apitacticsapp.model.CollaboratorType

data class CollaboratorDTO(
    val id: Int,
    val name: String,
    val type: CollaboratorType,
    val role: AccessRole
)