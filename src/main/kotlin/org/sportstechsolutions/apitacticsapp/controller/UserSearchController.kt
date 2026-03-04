package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
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
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Slice<PublicUserResponse>> {
        // Standardized exception throwing instead of raw ResponseEntity builds
        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthorizedException("You must be logged in to search for users")

        val results = userService.searchPublicUsers(query, page, size, currentUserId).map { user ->
            PublicUserResponse(
                id = user.id ?: 0,
                email = user.email,
                isPublic = user.isPublic
            )
        }

        return ResponseEntity.ok(results)
    }

    @PatchMapping("/me/public")
    fun updatePublicStatus(
        @Valid @RequestBody request: UpdateVisibilityRequest
    ): ResponseEntity<UserProfileResponse> {
        // Standardized exception throwing
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthorizedException("You must be logged in to modify your profile visibility")

        val updatedUser = userService.togglePublicStatus(userId, request.isPublic)

        return ResponseEntity.ok(
            UserProfileResponse(
                id = updatedUser.id ?: 0,
                email = updatedUser.email,
                isPublic = updatedUser.isPublic,
                message = "Profile visibility updated successfully"
            )
        )
    }
}