package org.sportstechsolutions.apitacticsapp.dtos

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.sportstechsolutions.apitacticsapp.model.User

data class SignupRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    @field:Size(min = 6, max = 50)
    val password: String
)

data class LoginRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val password: String
)

data class RefreshTokenRequest(
    @field:NotBlank
    val refreshToken: String
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)
data class UserResponse(
    val id: Int,
    val email: String,
    val role: String,
    val groups: List<GroupInfo> = emptyList(),
    val createdAt: String? = null,
    val lastLogin: String? = null
) {
    data class GroupInfo(
        val name: String
    )

    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id,
                email = user.email,
                role = user.role.name,
                groups = user.groups.map { GroupInfo(it.name) },
                createdAt = user.createdAt?.toString(),
                lastLogin = user.lastLogin?.toString()
            )
        }
    }
}

