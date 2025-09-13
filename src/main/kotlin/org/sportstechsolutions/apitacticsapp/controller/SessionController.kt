package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.SessionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/sessions")
class SessionController(private val sessionService: SessionService) {

    @GetMapping
    fun getMySessions(): ResponseEntity<List<SessionResponse>> {
        val userId = SecurityUtils.getCurrentUserId()
        return ResponseEntity.ok(sessionService.getSessionsByUserId(userId))
    }

    @GetMapping("/{id}")
    fun getSessionById(@PathVariable id: Int): ResponseEntity<SessionResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        return ResponseEntity.ok(sessionService.getSessionById(id, userId))
    }

    @PostMapping
    fun createSession(@RequestBody @Valid request: SessionRequest): ResponseEntity<SessionResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val session = sessionService.createSession(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(session)
    }

    @PutMapping("/{id}")
    fun updateSession(@PathVariable id: Int, @RequestBody @Valid request: SessionRequest): ResponseEntity<SessionResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val session = sessionService.updateSession(userId, id, request)
        return ResponseEntity.ok(session)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteSession(@PathVariable id: Int) {
        val userId = SecurityUtils.getCurrentUserId()
        sessionService.deleteSession(userId, id)
    }
}
