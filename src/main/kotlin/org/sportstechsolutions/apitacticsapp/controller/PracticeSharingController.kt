package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.CollaboratorDTO
import org.sportstechsolutions.apitacticsapp.dtos.RevokePracticeRequest
import org.sportstechsolutions.apitacticsapp.dtos.SharePracticeRequest
import org.sportstechsolutions.apitacticsapp.dtos.ShareResponse
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.PracticeSharingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/practices/share")
class PracticeSharingController(private val sharingService: PracticeSharingService) {

    @GetMapping("/{practiceId}/collaborators")
    fun getCollaborators(@PathVariable practiceId: Int): ResponseEntity<List<CollaboratorDTO>> {
        val currentUserId = SecurityUtils.getCurrentUserId()
        val collaborators = sharingService.getPracticeCollaborators(currentUserId, practiceId)
        return ResponseEntity.ok(collaborators)
    }

    // -----------------------------
    // SHARE WITH USER
    // -----------------------------
    @PostMapping("/user")
    fun shareWithUser(@RequestBody @Valid request: SharePracticeRequest): ResponseEntity<ShareResponse> {
        val currentUserId = SecurityUtils.getCurrentUserId()
        sharingService.sharePracticeWithUser(currentUserId, request.practiceId, request.targetId, request.role)
        val response = ShareResponse(
            sessionId = request.practiceId,
            targetId = request.targetId,
            role = request.role,
            message = "Practice shared successfully with user"
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @DeleteMapping("/user")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeUserAccess(@RequestBody @Valid request: RevokePracticeRequest) {
        val currentUserId = SecurityUtils.getCurrentUserId()
        sharingService.revokePracticeFromUser(currentUserId, request.practiceId, request.targetId)
    }

    // -----------------------------
    // SHARE WITH GROUP
    // -----------------------------
    @PostMapping("/group")
    fun shareWithGroup(@RequestBody @Valid request: SharePracticeRequest): ResponseEntity<ShareResponse> {
        val currentUserId = SecurityUtils.getCurrentUserId()
        sharingService.sharePracticeWithGroup(currentUserId, request.practiceId, request.targetId, request.role)
        val response = ShareResponse(
            sessionId = request.practiceId,
            targetId = request.targetId,
            role = request.role,
            message = "Practice shared successfully with group"
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @DeleteMapping("/group")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeGroupAccess(@RequestBody @Valid request: RevokePracticeRequest) {
        val currentUserId = SecurityUtils.getCurrentUserId()
        sharingService.revokePracticeFromGroup(currentUserId, request.practiceId, request.targetId)
    }
}
