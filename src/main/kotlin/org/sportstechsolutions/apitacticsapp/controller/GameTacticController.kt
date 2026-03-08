package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.slf4j.LoggerFactory
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

    private val log = LoggerFactory.getLogger(GameTacticController::class.java)

    @GetMapping
    fun getTabbedGameTactics(
        @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<TabbedResponse<GameTacticSummaryResponse>> {
        log.info("Get tabbed game tactics request received. Page: ${pageable.pageNumber}, Size: ${pageable.pageSize}")

        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("You must be logged in to view tabbed game tactics")

        return ResponseEntity.ok(gameTacticService.getGameTacticsForTabs(userId, pageable))
    }

    @PostMapping("/search")
    fun searchGameTactics(
        @RequestBody request: GameTacticSearchRequest,
        @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<PagedResponse<GameTacticSummaryResponse>> {
        log.info("Search game tactics request received. Term: '${request.searchTerm}', Scope: ${request.searchScope}")

        val userId = SecurityUtils.getCurrentUserId()

        val finalRequest = if (userId == null) {
            log.debug("Guest user detected for search, overriding scope to ALL_ACCESSIBLE.")
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
        log.info("Get game tactic request received for ID: $id")

        val userId = SecurityUtils.getCurrentUserId() ?: 0
        return ResponseEntity.ok(gameTacticService.getGameTacticById(id, userId, groupId))
    }

    @PostMapping
    fun createGameTactic(@RequestBody @Valid request: GameTacticRequest): ResponseEntity<GameTacticResponse> {
        log.info("Create game tactic request received. Name: '${request.name}'")

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
        log.info("Update game tactic request received for ID: $id")

        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("You must be logged in to update a game tactic")

        val updated = gameTacticService.updateGameTactic(userId, id, request, groupId)
        return ResponseEntity.ok(updated)
    }

    @PostMapping("/{id}/favorite")
    fun toggleFavorite(@PathVariable id: Int): ResponseEntity<Map<String, Boolean>> {
        log.info("Toggle favorite request received for Game Tactic ID: $id")

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
        log.info("Delete game tactic request received for ID: $id")

        val userId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("You must be logged in to delete content")

        gameTacticService.deleteGameTactic(userId, id, groupId)
    }
}