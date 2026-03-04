package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.UnauthenticatedException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.SearchScope
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.GameTacticService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/game-tactics")
class GameTacticController(private val gameTacticService: GameTacticService) {

    @GetMapping
    fun getTabbedGameTactics(
        @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<TabbedResponse<GameTacticSummaryResponse>> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("You must be logged in to view tabbed game tactics")

        return ResponseEntity.ok(gameTacticService.getGameTacticsForTabs(userId, pageable))
    }

    @PostMapping("/search")
    fun searchGameTactics(
        @RequestBody request: GameTacticSearchRequest,
        @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<PagedResponse<GameTacticSummaryResponse>> {
        val userId = SecurityUtils.getCurrentUserId()

        val finalRequest = if (userId == null) {
            request.copy(searchScope = SearchScope.ALL_ACCESSIBLE)
        } else {
            request
        }

        return ResponseEntity.ok(gameTacticService.searchGameTactics(userId ?: 0, finalRequest, pageable))
    }

    @GetMapping("/{id}")
    fun getGameTacticById(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<GameTacticResponse> {
        val userId = SecurityUtils.getCurrentUserId() ?: 0
        return ResponseEntity.ok(gameTacticService.getGameTacticById(id, userId, groupId))
    }

    @PostMapping
    fun createGameTactic(@RequestBody @Valid request: GameTacticRequest): ResponseEntity<GameTacticResponse> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("You must be logged in to create a game tactic")

        val tactic = gameTacticService.createGameTactic(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(tactic)
    }

    @PutMapping("/{id}")
    fun updateGameTactic(
        @PathVariable id: Int,
        @RequestBody @Valid request: GameTacticRequest,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<GameTacticResponse> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("You must be logged in to update a game tactic")

        val updated = gameTacticService.updateGameTactic(userId, id, request, groupId)
        return ResponseEntity.ok(updated)
    }

    @PostMapping("/{id}/favorite")
    fun toggleFavorite(@PathVariable id: Int): ResponseEntity<Map<String, Boolean>> {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("You must be logged in to favorite a game tactic")

        val isFavoriteNow = gameTacticService.toggleFavorite(userId, id)
        return ResponseEntity.ok(mapOf("isFavorite" to isFavoriteNow))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGameTactic(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ) {
        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("You must be logged in to delete content")

        gameTacticService.deleteGameTactic(userId, id, groupId)
    }
}