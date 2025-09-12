package org.sportstechsolutions.apitacticsapp.controller

import org.sportstechsolutions.apitacticsapp.dtos.GameTacticRequest
import org.sportstechsolutions.apitacticsapp.dtos.GameTacticResponse
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.GameTacticService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/game-tactics")
class GameTacticController(private val gameTacticService: GameTacticService) {

    @GetMapping
    fun getMyGameTactics(): ResponseEntity<List<GameTacticResponse>> {
        val userId = SecurityUtils.getCurrentUserId()
        return ResponseEntity.ok(gameTacticService.getGameTacticsByUserId(userId))
    }

    @GetMapping("/{id}")
    fun getGameTacticById(@PathVariable id: Int): ResponseEntity<GameTacticResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val tactic = gameTacticService.getGameTacticById(id, userId)
        return ResponseEntity.ok(tactic)
    }

    @PostMapping
    fun createGameTactic(@RequestBody request: GameTacticRequest): ResponseEntity<GameTacticResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val tactic = gameTacticService.createGameTactic(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(gameTacticService.getGameTacticsByUserId(userId).first { it.id == tactic.id })
    }

    @PutMapping("/{id}")
    fun updateGameTactic(@PathVariable id: Int, @RequestBody request: GameTacticRequest): ResponseEntity<GameTacticResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val tactic = gameTacticService.updateGameTactic(userId, id, request)
        return ResponseEntity.ok(gameTacticService.getGameTacticsByUserId(userId).first { it.id == tactic.id })
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGameTactic(@PathVariable id: Int) {
        val userId = SecurityUtils.getCurrentUserId()
        gameTacticService.deleteGameTactic(userId, id)
    }
}
