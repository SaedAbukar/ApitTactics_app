package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.PracticeService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/practices")
class PracticeController(private val practiceService: PracticeService) {

    // -----------------------------
    // GET TABBED PRACTICES
    // -----------------------------
    @GetMapping
    fun getTabbedPractices(): ResponseEntity<TabbedResponse<PracticeResponse>> {
        val userId = SecurityUtils.getCurrentUserId()
        return ResponseEntity.ok(practiceService.getPracticesForTabs(userId))
    }

    // -----------------------------
    // GET SINGLE PRACTICE
    // -----------------------------
    @GetMapping("/{id}")
    fun getPracticeById(
        @PathVariable id: Int,
        @RequestParam(required = false) groupId: Int? = null
    ): ResponseEntity<PracticeResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        return ResponseEntity.ok(practiceService.getPracticeById(id, userId, groupId))
    }

    // -----------------------------
    // CREATE PRACTICE
    // -----------------------------
    @PostMapping
    fun createPractice(@RequestBody @Valid request: PracticeRequest): ResponseEntity<PracticeResponse> {
        val userId = SecurityUtils.getCurrentUserId()
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
        val updated = practiceService.updatePractice(userId, id, request, groupId)
        return ResponseEntity.ok(updated)
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
        practiceService.deletePractice(userId, id, groupId)
    }
}
