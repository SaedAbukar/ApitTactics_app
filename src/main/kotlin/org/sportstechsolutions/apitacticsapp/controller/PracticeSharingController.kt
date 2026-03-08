package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.sportstechsolutions.apitacticsapp.dtos.CollaboratorDTO
import org.sportstechsolutions.apitacticsapp.dtos.RevokePracticeRequest
import org.sportstechsolutions.apitacticsapp.dtos.SharePracticeRequest
import org.sportstechsolutions.apitacticsapp.dtos.ShareResponse
import org.sportstechsolutions.apitacticsapp.exception.UnauthenticatedException
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.PracticeSharingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.security.access.AccessDeniedException

@RestController
@RequestMapping("/practices/share")
class PracticeSharingController(private val sharingService: PracticeSharingService) {

    private val log = LoggerFactory.getLogger(PracticeSharingController::class.java)

    @GetMapping("/{practiceId}/collaborators")
    fun getCollaborators(@PathVariable practiceId: Int): ResponseEntity<List<CollaboratorDTO>> {
        log.info("Get collaborators request received for Practice ID: $practiceId")

        // GUEST PROTECTION: Prevent anonymous users from seeing collaboration data
        val currentUserId = SecurityUtils.getCurrentUserId()
        if (currentUserId == null) {
            log.warn("Unauthorized attempt to view collaborators for Practice ID: $practiceId")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val collaborators = sharingService.getPracticeCollaborators(currentUserId, practiceId)
        return ResponseEntity.ok(collaborators)
    }

    // -----------------------------
    // SHARE WITH USER
    // -----------------------------
    @PostMapping("/user")
    fun shareWithUser(@RequestBody @Valid request: SharePracticeRequest): ResponseEntity<ShareResponse> {
        log.info("Share practice request received. Practice ID: ${request.practiceId}, Target User ID: ${request.targetId}, Role: ${request.role}")

        val currentUserId = SecurityUtils.getCurrentUserId()
        if (currentUserId == null) {
            log.warn("Unauthorized attempt to share Practice ID: ${request.practiceId}")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

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
        log.info("Revoke user access request received. Practice ID: ${request.practiceId}, Target User ID: ${request.targetId}")

        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("User must be logged in to modify sharing permissions")

        sharingService.revokePracticeFromUser(currentUserId, request.practiceId, request.targetId)
    }

    // -----------------------------
    // SHARE WITH GROUP
    // -----------------------------
    @PostMapping("/group")
    fun shareWithGroup(@RequestBody @Valid request: SharePracticeRequest): ResponseEntity<ShareResponse> {
        log.info("Share practice request received. Practice ID: ${request.practiceId}, Target Group ID: ${request.targetId}, Role: ${request.role}")

        val currentUserId = SecurityUtils.getCurrentUserId()
        if (currentUserId == null) {
            log.warn("Unauthorized attempt to share Practice ID: ${request.practiceId} with Group")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

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
        log.info("Revoke group access request received. Practice ID: ${request.practiceId}, Target Group ID: ${request.targetId}")

        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("User must be logged in to modify sharing permissions")

        sharingService.revokePracticeFromGroup(currentUserId, request.practiceId, request.targetId)
    }
}