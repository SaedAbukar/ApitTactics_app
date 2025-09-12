package org.sportstechsolutions.apitacticsapp.controller

import org.sportstechsolutions.apitacticsapp.dtos.PracticeRequest
import org.sportstechsolutions.apitacticsapp.dtos.PracticeResponse
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.PracticeService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/practices")
class PracticeController(private val practiceService: PracticeService) {

    @GetMapping
    fun getMyPractices(): ResponseEntity<List<PracticeResponse>> {
        val userId = SecurityUtils.getCurrentUserId()
        return ResponseEntity.ok(practiceService.getPracticesByUserId(userId))
    }

    @GetMapping("/{id}")
    fun getPracticeById(@PathVariable id: Int): ResponseEntity<PracticeResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val practice = practiceService.getPracticeById(id, userId)
        return ResponseEntity.ok(practice)
    }

    @PostMapping
    fun createPractice(@RequestBody request: PracticeRequest): ResponseEntity<PracticeResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val practice = practiceService.createPractice(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(practiceService.getPracticesByUserId(userId).first { it.id == practice.id })
    }

    @PutMapping("/{id}")
    fun updatePractice(@PathVariable id: Int, @RequestBody request: PracticeRequest): ResponseEntity<PracticeResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val practice = practiceService.updatePractice(userId, id, request)
        return ResponseEntity.ok(practiceService.getPracticesByUserId(userId).first { it.id == practice.id })
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePractice(@PathVariable id: Int) {
        val userId = SecurityUtils.getCurrentUserId()
        practiceService.deletePractice(userId, id)
    }
}
