package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.model.SearchScope
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.SessionService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.security.access.AccessDeniedException

@RestController
@RequestMapping("/sessions")
class SessionController(private val sessionService: SessionService) {

    // -----------------------------
    // GET TABBED SESSIONS (Returns Paginated Summaries)
    // -----------------------------
    @GetMapping
    fun getTabbedSessions(
        @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<TabbedResponse<SessionSummaryResponse>> {
        // Tabbed sessions (My Items, Shared, etc.) require an actual user
        val userId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return ResponseEntity.ok(sessionService.getSessionsForTabs(userId, pageable))
    }

    // -----------------------------
    // ADVANCED SEARCH (Returns Paginated Summaries)
    // -----------------------------
    @PostMapping("/search")
    fun searchSessions(
        @RequestBody @Valid request: SessionSearchRequest,
        @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<PagedResponse<SessionSummaryResponse>> {

        val userId = SecurityUtils.getCurrentUserId() // returns null for guests

        val finalRequest = if (userId == null) {
            // GUEST LOGIC: Force them to only see Premade content
            request.copy(searchScope = SearchScope.ALL_ACCESSIBLE)
        } else {
            // AUTHENTICATED: Allow user-defined scope
            request
        }

        // Pass userId or 0 to represent "Anonymous" to the service
        return ResponseEntity.ok(sessionService.searchSessions(userId ?: 0, finalRequest, pageable))
    }

    // -----------------------------
    // GET SINGLE SESSION (Returns Full Details & Increments Views)
    // -----------------------------
    @GetMapping("/{id}")
    fun getSessionById(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<SessionResponse> {
        // Guests (ID 0) are allowed to view public or premade sessions
        val userId = SecurityUtils.getCurrentUserId() ?: 0
        return ResponseEntity.ok(sessionService.getSessionById(id, userId, groupId))
    }

    // -----------------------------
    // CREATE SESSION
    // -----------------------------
    @PostMapping
    fun createSession(@RequestBody @Valid request: SessionRequest): ResponseEntity<SessionResponse> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

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
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val updated = sessionService.updateSession(userId, id, request, groupId)
        return ResponseEntity.ok(updated)
    }

    // -----------------------------
    // TOGGLE FAVORITE
    // -----------------------------
    @PostMapping("/{id}/favorite")
    fun toggleFavorite(@PathVariable id: Int): ResponseEntity<Map<String, Boolean>> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val isFavoriteNow = sessionService.toggleFavorite(userId, id)
        return ResponseEntity.ok(mapOf("isFavorite" to isFavoriteNow))
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
            ?: throw AccessDeniedException("You must be logged in to delete content")

        sessionService.deleteSession(userId, id, groupId)
    }
}