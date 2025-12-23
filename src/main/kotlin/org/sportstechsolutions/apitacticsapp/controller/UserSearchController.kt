package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.UserService
import org.springframework.data.domain.Slice
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserSearchController(private val userService: UserService) {

    @GetMapping("/search")
    fun search(
        @RequestParam query: String,
        @RequestParam page: Int,
        @RequestParam size: Int
    ): Slice<PublicUserResponse> {
        val currentUserId = SecurityUtils.getCurrentUserId()
        return userService.searchPublicUsers(query, page, size, currentUserId).map { user ->
            PublicUserResponse(
                id = user.id,
                email = user.email,
                isPublic = user.isPublic
            )
        }
    }

    @PatchMapping("/me/public")
    fun updatePublicStatus(
        @Valid @RequestBody request: UpdateVisibilityRequest
    ): ResponseEntity<UserProfileResponse> {
        val userId = SecurityUtils.getCurrentUserId()

        val updatedUser = userService.togglePublicStatus(userId, request.isPublic)

        return ResponseEntity.ok(
            UserProfileResponse(
                id = updatedUser.id,
                email = updatedUser.email,
                isPublic = updatedUser.isPublic,
                message = "Profile visibility updated successfully"
            )
        )
    }
}