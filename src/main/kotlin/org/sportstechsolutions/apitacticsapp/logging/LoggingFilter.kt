package org.sportstechsolutions.apitacticsapp.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class LoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(LoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val startTime = System.currentTimeMillis()
        val traceId = UUID.randomUUID().toString().substring(0, 8)

        try {
            // 1. Initialize MDC with the Trace ID
            MDC.put("traceId", traceId)
            MDC.put("userId", "Guest") // Default until Security validates them

            log.info("START -> [${request.method}] ${request.requestURI}")

            // 2. Pass down the chain (Security will run next)
            filterChain.doFilter(request, response)

        } finally {
            val duration = System.currentTimeMillis() - startTime
            log.info("END -> [${request.method}] ${request.requestURI} - Status: ${response.status} - ${duration}ms")

            // 3. CRITICAL: Prevent memory leaks
            MDC.clear()
        }
    }
}