package org.sportstechsolutions.apitacticsapp.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/")
    fun health(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "message" to "ApiTacticsApp is running on Render",
            "database" to "Connected"
        )
    }
}