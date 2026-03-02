package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.UserService
import org.springframework.data.domain.Slice
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/users")
class UserSearchController(private val userService: UserService) {

    @GetMapping("/search")
    fun search(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Slice<PublicUserResponse>> {
        // GUEST PROTECTION: Only logged-in users can search for other users
        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

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
        // GUEST PROTECTION: Must be logged in to modify own profile visibility
        val userId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

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