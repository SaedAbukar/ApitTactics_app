package org.sportstechsolutions.apitacticsapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class ApiTacticsAppApplication

fun main(args: Array<String>) {
    runApplication<ApiTacticsAppApplication>(*args)
}
