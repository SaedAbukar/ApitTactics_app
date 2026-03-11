package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.UnauthenticatedException
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

    private val log = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/oauth2/exchange")
    fun exchangeOAuth2Code(@Valid @RequestBody request: OAuth2ExchangeRequest): ResponseEntity<TokenResponse> {
        log.info("OAuth2 token exchange request received.")
        val tokens = authService.exchangeOAuth2Code(request.code)
        return ResponseEntity.ok(TokenResponse(tokens.accessToken, tokens.refreshToken))
    }

    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<UserResponse> {
        log.info("Signup request received for email: ${request.email}")
        // ---> PASSED NAME HERE <---
        val user = authService.register(request.name, request.email, request.password)
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        log.info("Login request received for email: ${request.email}")
        val tokens = authService.login(request.email, request.password)
        return ResponseEntity.ok(TokenResponse(tokens.accessToken, tokens.refreshToken))
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<TokenResponse> {
        log.info("Refresh token request received")
        val tokens = authService.refresh(request.refreshToken)
        return ResponseEntity.ok(TokenResponse(tokens.accessToken, tokens.refreshToken))
    }

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: LogoutRequest): ResponseEntity<Unit> {
        log.info("Logout request received")
        authService.logout(request.refreshToken)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me")
    fun me(): ResponseEntity<UserResponse> {
        log.info("Current user profile request received")
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("You are not authenticated. Please log in.")
        val user = userService.getUserWithGroupsById(userId)
        return ResponseEntity.ok(UserResponse.from(user))
    }
}