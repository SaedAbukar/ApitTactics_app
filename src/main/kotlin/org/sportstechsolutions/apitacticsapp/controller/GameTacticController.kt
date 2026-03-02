package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.model.SearchScope
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.GameTacticService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.security.access.AccessDeniedException

@RestController
@RequestMapping("/game-tactics")
class GameTacticController(private val gameTacticService: GameTacticService) {

    // -----------------------------
    // GET TABBED GAME TACTICS (Returns Paginated Summaries)
    // -----------------------------
    @GetMapping
    fun getTabbedGameTactics(
        @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<TabbedResponse<GameTacticSummaryResponse>> {
        // Authenticated users only
        val userId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return ResponseEntity.ok(gameTacticService.getGameTacticsForTabs(userId, pageable))
    }

    // -----------------------------
    // ADVANCED SEARCH (Returns Paginated Summaries)
    // -----------------------------
    @PostMapping("/search")
    fun searchGameTactics(
        @RequestBody request: GameTacticSearchRequest,
        @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<PagedResponse<GameTacticSummaryResponse>> {
        val userId = SecurityUtils.getCurrentUserId()

        // Guest logic: Override scope to PREMADE if not logged in
        val finalRequest = if (userId == null) {
            request.copy(searchScope = SearchScope.ALL_ACCESSIBLE)
        } else {
            request
        }

        return ResponseEntity.ok(gameTacticService.searchGameTactics(userId ?: 0, finalRequest, pageable))
    }

    // -----------------------------
    // GET SINGLE GAME TACTIC (Returns Full Details & Increments Views)
    // -----------------------------
    @GetMapping("/{id}")
    fun getGameTacticById(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<GameTacticResponse> {
        // Allow guests (userId 0) to view specific public/premade tactics
        val userId = SecurityUtils.getCurrentUserId() ?: 0
        return ResponseEntity.ok(gameTacticService.getGameTacticById(id, userId, groupId))
    }

    // -----------------------------
    // CREATE GAME TACTIC
    // -----------------------------
    @PostMapping
    fun createGameTactic(@RequestBody @Valid request: GameTacticRequest): ResponseEntity<GameTacticResponse> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val tactic = gameTacticService.createGameTactic(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(tactic)
    }

    // -----------------------------
    // UPDATE GAME TACTIC
    // -----------------------------
    @PutMapping("/{id}")
    fun updateGameTactic(
        @PathVariable id: Int,
        @RequestBody @Valid request: GameTacticRequest,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<GameTacticResponse> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val updated = gameTacticService.updateGameTactic(userId, id, request, groupId)
        return ResponseEntity.ok(updated)
    }

    // -----------------------------
    // TOGGLE FAVORITE
    // -----------------------------
    @PostMapping("/{id}/favorite")
    fun toggleFavorite(@PathVariable id: Int): ResponseEntity<Map<String, Boolean>> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val isFavoriteNow = gameTacticService.toggleFavorite(userId, id)
        return ResponseEntity.ok(mapOf("isFavorite" to isFavoriteNow))
    }

    // -----------------------------
    // DELETE GAME TACTIC
    // -----------------------------
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGameTactic(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ) {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw AccessDeniedException("User must be logged in to delete content")

        gameTacticService.deleteGameTactic(userId, id, groupId)
    }
}