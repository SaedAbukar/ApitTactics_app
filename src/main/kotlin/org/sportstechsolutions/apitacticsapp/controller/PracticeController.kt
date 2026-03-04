package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.SearchScope
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.PracticeService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/practices")
class PracticeController(private val practiceService: PracticeService) {

    @GetMapping
    fun getTabbedPractices(
        @PageableDefault(size = 5, page = 0) pageable: Pageable
    ): ResponseEntity<TabbedResponse<PracticeSummaryResponse>> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthorizedException("You must be logged in to view tabbed practices")

        return ResponseEntity.ok(practiceService.getPracticesForTabs(userId, pageable))
    }

    @PostMapping("/search")
    fun searchPractices(
        @RequestBody request: PracticeSearchRequest,
        @PageableDefault(size = 5, page = 0) pageable: Pageable
    ): ResponseEntity<PagedResponse<PracticeSummaryResponse>> {
        val userId = SecurityUtils.getCurrentUserId()

        val finalRequest = if (userId == null) {
            request.copy(searchScope = SearchScope.ALL_ACCESSIBLE)
        } else {
            request
        }

        return ResponseEntity.ok(practiceService.searchPractices(userId ?: 0, finalRequest, pageable))
    }

    @GetMapping("/{id}")
    fun getPracticeById(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<PracticeResponse> {
        val userId = SecurityUtils.getCurrentUserId() ?: 0
        return ResponseEntity.ok(practiceService.getPracticeById(id, userId, groupId))
    }

    @PostMapping
    fun createPractice(@RequestBody @Valid request: PracticeRequest): ResponseEntity<PracticeResponse> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthorizedException("You must be logged in to create a practice")

        val practice = practiceService.createPractice(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(practice)
    }

    @PutMapping("/{id}")
    fun updatePractice(
        @PathVariable id: Int,
        @RequestBody @Valid request: PracticeRequest,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<PracticeResponse> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthorizedException("You must be logged in to update a practice")

        val updated = practiceService.updatePractice(userId, id, request, groupId)
        return ResponseEntity.ok(updated)
    }

    @PostMapping("/{id}/favorite")
    fun toggleFavorite(@PathVariable id: Int): ResponseEntity<Map<String, Boolean>> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthorizedException("You must be logged in to favorite a practice")

        val isFavoriteNow = practiceService.toggleFavorite(userId, id)
        return ResponseEntity.ok(mapOf("isFavorite" to isFavoriteNow))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePractice(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ) {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthorizedException("You must be logged in to delete content")

        practiceService.deletePractice(userId, id, groupId)
    }
}