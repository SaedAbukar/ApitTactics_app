package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.GameTacticRepository
import org.sportstechsolutions.apitacticsapp.dtos.EntityMappers
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GameTacticService(
    private val gameTacticRepository: GameTacticRepository,
    private val entityMappers: EntityMappers
) {

    @Transactional
    fun createGameTactic(userId: Int, request: GameTacticRequest): GameTacticResponse {
        val gameTactic = GameTactic(
            name = request.name,
            description = request.description,
            is_premade = request.isPremade,
            owner = User(id = userId)
        )

        val sessions = request.sessions.map { entityMappers.toSession(it, gameTactic) }
        sessions.forEach { it.gameTactics.add(gameTactic) }
        gameTactic.sessions.addAll(sessions)

        val saved = gameTacticRepository.save(gameTactic)
        return entityMappers.loadFullGameTactic(saved)
    }

    @Transactional
    fun updateGameTactic(userId: Int, gameTacticId: Int, request: GameTacticRequest): GameTacticResponse {
        val gameTactic = gameTacticRepository.findById(gameTacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        if (gameTactic.owner?.id != userId) throw UnauthorizedException("Not allowed")

        gameTactic.name = request.name
        gameTactic.description = request.description
        gameTactic.is_premade = request.isPremade

        gameTactic.sessions.forEach { it.gameTactics.remove(gameTactic) }
        gameTactic.sessions.clear()

        val newSessions = request.sessions.map { entityMappers.toSession(it, gameTactic) }
        newSessions.forEach { it.gameTactics.add(gameTactic) }
        gameTactic.sessions.addAll(newSessions)

        val updated = gameTacticRepository.save(gameTactic)
        return entityMappers.loadFullGameTactic(updated)
    }

    @Transactional
    fun deleteGameTactic(userId: Int, gameTacticId: Int) {
        val gameTactic = gameTacticRepository.findById(gameTacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        if (gameTactic.owner?.id != userId) throw UnauthorizedException("Not allowed")

        gameTacticRepository.delete(gameTactic)
    }

    @Transactional(readOnly = true)
    fun getGameTacticsByUserId(userId: Int): List<GameTacticResponse> {
        return gameTacticRepository.findByOwnerId(userId)
            .map { entityMappers.loadFullGameTactic(it) }
    }

    @Transactional(readOnly = true)
    fun getGameTacticById(gameTacticId: Int, userId: Int): GameTacticResponse {
        val gameTactic = gameTacticRepository.findById(gameTacticId)
            .orElseThrow { ResourceNotFoundException("Game tactic not found") }

        if (gameTactic.owner?.id != userId) throw UnauthorizedException("Not allowed")

        return entityMappers.loadFullGameTactic(gameTactic)
    }
}
