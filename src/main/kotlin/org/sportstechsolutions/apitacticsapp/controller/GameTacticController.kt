package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.GameTacticService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/game-tactics")
class GameTacticController(private val gameTacticService: GameTacticService) {

    // -----------------------------
    // GET TABBED GAME TACTICS
    // -----------------------------
    @GetMapping
    fun getTabbedGameTactics(): ResponseEntity<TabbedResponse<GameTacticResponse>> {
        val userId = SecurityUtils.getCurrentUserId()
        return ResponseEntity.ok(gameTacticService.getGameTacticsForTabs(userId))
    }

    // -----------------------------
    // GET SINGLE GAME TACTIC
    // -----------------------------
    @GetMapping("/{id}")
    fun getGameTacticById(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<GameTacticResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        return ResponseEntity.ok(gameTacticService.getGameTacticById(id, userId, groupId))
    }

    // -----------------------------
    // CREATE GAME TACTIC
    // -----------------------------
    @PostMapping
    fun createGameTactic(@RequestBody @Valid request: GameTacticRequest): ResponseEntity<GameTacticResponse> {
        val userId = SecurityUtils.getCurrentUserId()
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
        val updated = gameTacticService.updateGameTactic(userId, id, request, groupId)
        return ResponseEntity.ok(updated)
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
        gameTacticService.deleteGameTactic(userId, id, groupId)
    }
}
