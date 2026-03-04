package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.CollaboratorDTO
import org.sportstechsolutions.apitacticsapp.dtos.RevokeSessionRequest
import org.sportstechsolutions.apitacticsapp.dtos.ShareResponse
import org.sportstechsolutions.apitacticsapp.dtos.ShareSessionRequest
import org.sportstechsolutions.apitacticsapp.exception.UnauthenticatedException
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.SessionSharingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.security.access.AccessDeniedException

@RestController
@RequestMapping("/sessions/share")
class SessionSharingController(private val sharingService: SessionSharingService) {

    // -----------------------------
    // GET COLLABORATORS
    // -----------------------------
    @GetMapping("/{sessionId}/collaborators")
    fun getCollaborators(@PathVariable sessionId: Int): ResponseEntity<List<CollaboratorDTO>> {
        // GUEST PROTECTION: Only authenticated users can see collaboration lists
        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val collaborators = sharingService.getSessionCollaborators(currentUserId, sessionId)
        return ResponseEntity.ok(collaborators)
    }

    // -----------------------------
    // SHARE WITH USER
    // -----------------------------
    @PostMapping("/user")
    fun shareWithUser(@RequestBody @Valid request: ShareSessionRequest): ResponseEntity<ShareResponse> {
        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        sharingService.shareSessionWithUser(currentUserId, request.sessionId, request.targetId, request.role)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ShareResponse(request.sessionId, request.targetId, request.role, "Shared with user")
        )
    }

    @DeleteMapping("/user")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeUserAccess(@RequestBody @Valid request: RevokeSessionRequest) {
        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("User must be logged in to revoke access")

        sharingService.revokeSessionFromUser(currentUserId, request.sessionId, request.targetId)
    }

    // -----------------------------
    // SHARE WITH GROUP
    // -----------------------------
    @PostMapping("/group")
    fun shareWithGroup(@RequestBody @Valid request: ShareSessionRequest): ResponseEntity<ShareResponse> {
        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        sharingService.shareSessionWithGroup(currentUserId, request.sessionId, request.targetId, request.role)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ShareResponse(request.sessionId, request.targetId, request.role, "Shared with group")
        )
    }

    @DeleteMapping("/group")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeGroupAccess(@RequestBody @Valid request: RevokeSessionRequest) {
        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: throw UnauthenticatedException("User must be logged in to revoke access")

        sharingService.revokeSessionFromGroup(currentUserId, request.sessionId, request.targetId)
    }
}