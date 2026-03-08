package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.UnauthenticatedException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.SearchScope
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.SessionService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/sessions")
class SessionController(private val sessionService: SessionService) {

    private val log = LoggerFactory.getLogger(SessionController::class.java)

    @GetMapping
    fun getTabbedSessions(
        @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<TabbedResponse<SessionSummaryResponse>> {
        log.info("Get tabbed sessions request received. Page: ${pageable.pageNumber}, Size: ${pageable.pageSize}")

        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("You must be logged in to view tabbed sessions")

        return ResponseEntity.ok(sessionService.getSessionsForTabs(userId, pageable))
    }

    @PostMapping("/search")
    fun searchSessions(
        @RequestBody @Valid request: SessionSearchRequest,
        @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<PagedResponse<SessionSummaryResponse>> {
        log.info("Search sessions request received. Term: '${request.searchTerm}', Scope: ${request.searchScope}")

        val userId = SecurityUtils.getCurrentUserId()

        val finalRequest = if (userId == null) {
            log.debug("Guest user detected for search, overriding scope to ALL_ACCESSIBLE.")
            request.copy(searchScope = SearchScope.ALL_ACCESSIBLE)
        } else {
            request
        }

        return ResponseEntity.ok(sessionService.searchSessions(userId ?: 0, finalRequest, pageable))
    }

    @GetMapping("/{id}")
    fun getSessionById(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<SessionResponse> {
        log.info("Get session request received for ID: $id")

        val userId = SecurityUtils.getCurrentUserId() ?: 0
        return ResponseEntity.ok(sessionService.getSessionById(id, userId, groupId))
    }

    @PostMapping
    fun createSession(@RequestBody @Valid request: SessionRequest): ResponseEntity<SessionResponse> {
        log.info("Create session request received. Name: '${request.name}'")

        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("You must be logged in to create a session")

        val session = sessionService.createSession(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(session)
    }

    @PutMapping("/{id}")
    fun updateSession(
        @PathVariable id: Int,
        @RequestBody @Valid request: SessionRequest,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<SessionResponse> {
        log.info("Update session request received for ID: $id")

        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("You must be logged in to update a session")

        val updated = sessionService.updateSession(userId, id, request, groupId)
        return ResponseEntity.ok(updated)
    }

    @PostMapping("/{id}/favorite")
    fun toggleFavorite(@PathVariable id: Int): ResponseEntity<Map<String, Boolean>> {
        log.info("Toggle favorite request received for Session ID: $id")

        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("You must be logged in to favorite a session")

        val isFavoriteNow = sessionService.toggleFavorite(userId, id)
        return ResponseEntity.ok(mapOf("isFavorite" to isFavoriteNow))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteSession(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ) {
        log.info("Delete session request received for ID: $id")

        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("You must be logged in to delete content")

        sessionService.deleteSession(userId, id, groupId)
    }
}