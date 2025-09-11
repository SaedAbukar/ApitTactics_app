package org.sportstechsolutions.apitacticsapp.dtos

data class UserGroupResponse(
    val id: Int,
    val name: String
) {
    companion object {
        fun from(group: org.sportstechsolutions.apitacticsapp.model.UserGroup) =
            UserGroupResponse(
                id = group.id,
                name = group.name
            )
    }
}