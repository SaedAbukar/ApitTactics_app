package org.sportstechsolutions.apitacticsapp.controller

import jakarta.validation.Valid
import org.sportstechsolutions.apitacticsapp.dtos.CollaboratorDTO
import org.sportstechsolutions.apitacticsapp.dtos.RevokeGameTacticRequest
import org.sportstechsolutions.apitacticsapp.dtos.ShareGameTacticRequest
import org.sportstechsolutions.apitacticsapp.dtos.ShareResponse
import org.sportstechsolutions.apitacticsapp.security.SecurityUtils
import org.sportstechsolutions.apitacticsapp.service.GameTacticSharingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.security.access.AccessDeniedException

@RestController
@RequestMapping("/game-tactics/share")
class GameTacticSharingController(private val sharingService: GameTacticSharingService) {

    @GetMapping("/{tacticId}/collaborators")
    fun getCollaborators(@PathVariable tacticId: Int): ResponseEntity<List<CollaboratorDTO>> {
        // GUEST PROTECTION: Guests cannot see who has access to a tactic
        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val collaborators = sharingService.getGameTacticCollaborators(currentUserId, tacticId)
        return ResponseEntity.ok(collaborators)
    }

    // -----------------------------
    // SHARE WITH USER
    // -----------------------------
    @PostMapping("/user")
    fun shareWithUser(@RequestBody @Valid request: ShareGameTacticRequest): ResponseEntity<ShareResponse> {
        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        sharingService.shareGameTacticWithUser(currentUserId, request.gameTacticId, request.targetId, request.role)
        val response = ShareResponse(
            sessionId = request.gameTacticId,
            targetId = request.targetId,
            role = request.role,
            message = "Game tactic shared successfully with user"
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @DeleteMapping("/user")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeUserAccess(@RequestBody @Valid request: RevokeGameTacticRequest) {
        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: throw AccessDeniedException("User must be logged in to modify sharing permissions")

        sharingService.revokeGameTacticFromUser(currentUserId, request.gameTacticId, request.targetId)
    }

    // -----------------------------
    // SHARE WITH GROUP
    // -----------------------------
    @PostMapping("/group")
    fun shareWithGroup(@RequestBody @Valid request: ShareGameTacticRequest): ResponseEntity<ShareResponse> {
        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        sharingService.shareGameTacticWithGroup(currentUserId, request.gameTacticId, request.targetId, request.role)
        val response = ShareResponse(
            sessionId = request.gameTacticId,
            targetId = request.targetId,
            role = request.role,
            message = "Game tactic shared successfully with group"
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @DeleteMapping("/group")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeGroupAccess(@RequestBody @Valid request: RevokeGameTacticRequest) {
        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: throw AccessDeniedException("User must be logged in to modify sharing permissions")

        sharingService.revokeGameTacticFromGroup(currentUserId, request.gameTacticId, request.targetId)
    }
}