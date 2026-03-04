package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.security.AuthService
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
        // 1. Standardized Unauthorized check
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthorizedException("Not authenticated")

        // 2. Fetch the user.
        // Note: The null check and ResponseStatusException were removed because
        // the refactored UserService now safely throws ResourceNotFoundException automatically!
        val user = userService.getUserWithGroupsById(userId)

        return ResponseEntity.ok(UserResponse.from(user))
    }
}