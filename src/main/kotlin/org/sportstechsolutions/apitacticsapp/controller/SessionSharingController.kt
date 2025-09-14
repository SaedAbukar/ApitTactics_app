package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.RevokeSessionRequest
import org.sportstechsolutions.apitacticsapp.dtos.ShareResponse
import org.sportstechsolutions.apitacticsapp.dtos.ShareSessionRequest
import org.sportstechsolutions.apitacticsapp.model.AccessRole
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.SessionSharingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/sessions/share")
class SessionSharingController(private val sharingService: SessionSharingService) {
    // -----------------------------
    // SHARE WITH USER
    // -----------------------------
    @PostMapping("/user")
    fun shareWithUser(@RequestBody @Valid request: ShareSessionRequest): ResponseEntity<ShareResponse> {
        val currentUserId = SecurityUtils.getCurrentUserId()
        sharingService.shareSessionWithUser(currentUserId, request.sessionId, request.targetId, request.role)
        val response = ShareResponse(
            sessionId = request.sessionId,
            targetId = request.targetId,
            role = request.role,
            message = "Session shared successfully with user"
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @DeleteMapping("/user")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeUserAccess(@RequestBody @Valid request: RevokeSessionRequest) {
        val currentUserId = SecurityUtils.getCurrentUserId()
        sharingService.revokeSessionFromUser(currentUserId, request.sessionId, request.targetId)
    }

    // -----------------------------
    // SHARE WITH GROUP
    // -----------------------------
    @PostMapping("/group")
    fun shareWithGroup(@RequestBody @Valid request: ShareSessionRequest): ResponseEntity<ShareResponse> {
        val currentUserId = SecurityUtils.getCurrentUserId()
        sharingService.shareSessionWithGroup(currentUserId, request.sessionId, request.targetId, request.role)
        val response = ShareResponse(
            sessionId = request.sessionId,
            targetId = request.targetId,
            role = request.role,
            message = "Session shared successfully with group"
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @DeleteMapping("/group")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeGroupAccess(@RequestBody @Valid request: RevokeSessionRequest) {
        val currentUserId = SecurityUtils.getCurrentUserId()
        sharingService.revokeSessionFromGroup(currentUserId, request.sessionId, request.targetId)
    }
}
