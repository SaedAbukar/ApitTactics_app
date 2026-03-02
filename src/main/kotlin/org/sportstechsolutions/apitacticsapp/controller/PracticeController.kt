package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.model.SearchScope
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.PracticeService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.security.access.AccessDeniedException

@RestController
@RequestMapping("/practices")
class PracticeController(private val practiceService: PracticeService) {

    // -----------------------------
    // GET TABBED PRACTICES (Returns Paginated Summaries)
    // -----------------------------
    @GetMapping
    fun getTabbedPractices(
        @PageableDefault(size = 5, page = 0) pageable: Pageable
    ): ResponseEntity<TabbedResponse<PracticeSummaryResponse>> {
        // PERSONALIZED: Requires a logged-in user
        val userId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return ResponseEntity.ok(practiceService.getPracticesForTabs(userId, pageable))
    }

    // -----------------------------
    // ADVANCED SEARCH (Returns Paginated Summaries)
    // -----------------------------
    @PostMapping("/search")
    fun searchPractices(
        @RequestBody request: PracticeSearchRequest,
        @PageableDefault(size = 5, page = 0) pageable: Pageable
    ): ResponseEntity<PagedResponse<PracticeSummaryResponse>> {
        val userId = SecurityUtils.getCurrentUserId()

        // GUEST ENFORCEMENT: If no user, force them into the PREMADE scope
        val finalRequest = if (userId == null) {
            request.copy(searchScope = SearchScope.ALL_ACCESSIBLE)
        } else {
            request
        }

        // Pass 0 to the service for anonymous users
        return ResponseEntity.ok(practiceService.searchPractices(userId ?: 0, finalRequest, pageable))
    }

    // -----------------------------
    // GET SINGLE PRACTICE (Returns Full Details & Increments Views)
    // -----------------------------
    @GetMapping("/{id}")
    fun getPracticeById(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<PracticeResponse> {
        // GUEST ALLOWED: ID 0 can view public or premade practices
        val userId = SecurityUtils.getCurrentUserId() ?: 0
        return ResponseEntity.ok(practiceService.getPracticeById(id, userId, groupId))
    }

    // -----------------------------
    // CREATE PRACTICE
    // -----------------------------
    @PostMapping
    fun createPractice(@RequestBody @Valid request: PracticeRequest): ResponseEntity<PracticeResponse> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val practice = practiceService.createPractice(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(practice)
    }

    // -----------------------------
    // UPDATE PRACTICE
    // -----------------------------
    @PutMapping("/{id}")
    fun updatePractice(
        @PathVariable id: Int,
        @RequestBody @Valid request: PracticeRequest,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<PracticeResponse> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val updated = practiceService.updatePractice(userId, id, request, groupId)
        return ResponseEntity.ok(updated)
    }

    // -----------------------------
    // TOGGLE FAVORITE
    // -----------------------------
    @PostMapping("/{id}/favorite")
    fun toggleFavorite(@PathVariable id: Int): ResponseEntity<Map<String, Boolean>> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val isFavoriteNow = practiceService.toggleFavorite(userId, id)
        return ResponseEntity.ok(mapOf("isFavorite" to isFavoriteNow))
    }

    // -----------------------------
    // DELETE PRACTICE
    // -----------------------------
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePractice(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ) {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw AccessDeniedException("User must be logged in to delete content")

        practiceService.deletePractice(userId, id, groupId)
    }
}