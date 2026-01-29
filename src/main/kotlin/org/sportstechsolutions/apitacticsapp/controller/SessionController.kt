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

    // -----------------------------
    // GET TABBED SESSIONS (FIXED RETURN TYPE)
    // -----------------------------
    @GetMapping
    fun getTabbedSessions(): ResponseEntity<TabbedResponse<SessionSummaryResponse>> { // <--- Changed here
        val userId = SecurityUtils.getCurrentUserId()
        return ResponseEntity.ok(sessionService.getSessionsForTabs(userId))
    }

    // -----------------------------
    // GET SINGLE SESSION
    // -----------------------------
    @GetMapping("/{id}")
    fun getSessionById(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<SessionResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        return ResponseEntity.ok(sessionService.getSessionById(id, userId, groupId))
    }

    // -----------------------------
    // CREATE SESSION
    // -----------------------------
    @PostMapping
    fun createSession(@RequestBody @Valid request: SessionRequest): ResponseEntity<SessionResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val session = sessionService.createSession(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(session)
    }

    // -----------------------------
    // UPDATE SESSION
    // -----------------------------
    @PutMapping("/{id}")
    fun updateSession(
        @PathVariable id: Int,
        @RequestBody @Valid request: SessionRequest,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<SessionResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val updated = sessionService.updateSession(userId, id, request, groupId)
        return ResponseEntity.ok(updated)
    }

    // -----------------------------
    // DELETE SESSION
    // -----------------------------
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteSession(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ) {
        val userId = SecurityUtils.getCurrentUserId()
        sessionService.deleteSession(userId, id, groupId)
    }
}