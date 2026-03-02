package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.security.AuthService
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService
) {

    // ---------------- SIGNUP ----------------
    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<UserResponse> {
        val user = authService.register(request.email, request.password)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(UserResponse.from(user))
    }

    // ---------------- LOGIN ----------------
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        val tokens = authService.login(request.email, request.password)

        val response = TokenResponse(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken
        )

        return ResponseEntity.ok(response)
    }

    // ---------------- REFRESH ----------------
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<TokenResponse> {
        val tokens = authService.refresh(request.refreshToken)

        val response = TokenResponse(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken
        )

        return ResponseEntity.ok(response)
    }

    // ---------------- CURRENT USER ----------------
    @GetMapping("/me")
    fun me(): ResponseEntity<UserResponse> {
        // 1. Get userId safely. If null (Guest), return 401 Unauthorized
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated")

        // 2. Fetch the user using the non-null ID
        val user = userService.getUserWithGroupsById(userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        return ResponseEntity.ok(UserResponse.from(user))
    }
}