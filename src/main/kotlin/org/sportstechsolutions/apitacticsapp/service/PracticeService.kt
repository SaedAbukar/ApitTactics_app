package org.sportstechsolutions.apitacticsapp.service

import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.exception.ResourceNotFoundException
import org.sportstechsolutions.apitacticsapp.exception.UnauthorizedException
import org.sportstechsolutions.apitacticsapp.model.*
import org.sportstechsolutions.apitacticsapp.repository.PracticeRepository
import org.sportstechsolutions.apitacticsapp.dtos.EntityMappers
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PracticeService(
    private val practiceRepository: PracticeRepository,
    private val entityMappers: EntityMappers
) {

    @Transactional
    fun createPractice(userId: Int, request: PracticeRequest): PracticeResponse {
        val practice = Practice(
            name = request.name,
            description = request.description,
            is_premade = request.isPremade,
            owner = User(id = userId)
        )

        val sessions = request.sessions.map { entityMappers.toSession(it, practice) }
        sessions.forEach { it.practices.add(practice) }
        practice.sessions.addAll(sessions)

        val saved = practiceRepository.save(practice)
        return entityMappers.loadFullPractice(saved)
    }

    @Transactional
    fun updatePractice(userId: Int, practiceId: Int, request: PracticeRequest): PracticeResponse {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        if (practice.owner?.id != userId) throw UnauthorizedException("Not allowed")

        practice.name = request.name
        practice.description = request.description
        practice.is_premade = request.isPremade

        practice.sessions.forEach { it.practices.remove(practice) }
        practice.sessions.clear()

        val newSessions = request.sessions.map { entityMappers.toSession(it, practice) }
        newSessions.forEach { it.practices.add(practice) }
        practice.sessions.addAll(newSessions)

        val updated = practiceRepository.save(practice)
        return entityMappers.loadFullPractice(updated)
    }

    @Transactional
    fun deletePractice(userId: Int, practiceId: Int) {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        if (practice.owner?.id != userId) throw UnauthorizedException("Not allowed")

        practiceRepository.delete(practice)
    }

    @Transactional(readOnly = true)
    fun getPracticesByUserId(userId: Int): List<PracticeResponse> {
        return practiceRepository.findByOwnerId(userId)
            .map { entityMappers.loadFullPractice(it) }
    }

    @Transactional(readOnly = true)
    fun getPracticeById(practiceId: Int, userId: Int): PracticeResponse {
        val practice = practiceRepository.findById(practiceId)
            .orElseThrow { ResourceNotFoundException("Practice not found") }

        if (practice.owner?.id != userId) throw UnauthorizedException("Not allowed")

        return entityMappers.loadFullPractice(practice)
    }
}
