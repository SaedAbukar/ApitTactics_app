package org.sportstechsolutions.apitacticsapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
@EnableScheduling
class ApiTacticsAppApplication

fun main(args: Array<String>) {
    runApplication<ApiTacticsAppApplication>(*args)
}
