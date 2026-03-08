package org.sportstechsolutions.apitacticsapp.exception

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.Instant

// Standard API error response with timestamp and path for easier debugging
data class ApiError(
    val timestamp: String = Instant.now().toString(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val fieldErrors: List<FieldValidationError>? = null
)

data class FieldValidationError(
    val field: String,
    val rejectedValue: Any?,
    val message: String
)

@ControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // ------------------------------------------------------------
    // 1. SPRING VALIDATION ERRORS (@Valid DTO constraints)
    // ------------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.warn("Validation error on [${request.method}] ${request.requestURI}: ${ex.message}")

        val errors = ex.bindingResult.allErrors.mapNotNull { error ->
            if (error is FieldError) {
                FieldValidationError(
                    field = error.field,
                    rejectedValue = error.rejectedValue,
                    message = error.defaultMessage ?: "Invalid value"
                )
            } else null
        }

        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = "Validation failed for one or more fields",
            path = request.requestURI,
            fieldErrors = errors
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError)
    }

    // ------------------------------------------------------------
    // 2. BAD REQUEST ERRORS (Malformed JSON, Type Mismatch, Missing Params)
    // ------------------------------------------------------------
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.warn("Malformed JSON received on [${request.method}] ${request.requestURI}: ${ex.message}")
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request. Please check your syntax.", request)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val requiredType = ex.requiredType?.simpleName ?: "Unknown"
        val message = "Parameter '${ex.name}' should be of type $requiredType"
        log.warn("Type mismatch on [${request.method}] ${request.requestURI}: $message")
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParams(ex: MissingServletRequestParameterException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val message = "Required request parameter '${ex.parameterName}' is missing"
        log.warn("Missing parameter on [${request.method}] ${request.requestURI}: $message")
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.warn("Illegal argument on [${request.method}] ${request.requestURI}: ${ex.message}")
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request parameters", request)
    }

    // ------------------------------------------------------------
    // 3. DATABASE EXCEPTIONS
    // ------------------------------------------------------------
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException, request: HttpServletRequest): ResponseEntity<ApiError> {
        // Logged as an ERROR because this indicates a structural database conflict (e.g., missing foreign key, duplicate unique index)
        log.error("Database constraint violation on [${request.method}] ${request.requestURI}", ex)
        return buildErrorResponse(HttpStatus.CONFLICT, "Database constraint violation. A record may already exist or is currently in use.", request)
    }

    // ------------------------------------------------------------
    // 4. CUSTOM DOMAIN EXCEPTIONS
    // ------------------------------------------------------------
    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.warn("Resource conflict on [${request.method}] ${request.requestURI}: ${ex.message}")
        return buildErrorResponse(HttpStatus.CONFLICT, ex.message ?: "Resource is currently in use", request)
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.warn("Resource not found on [${request.method}] ${request.requestURI}: ${ex.message}")
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found", request)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.warn("Illegal state encountered on [${request.method}] ${request.requestURI}: ${ex.message}")
        return buildErrorResponse(HttpStatus.CONFLICT, ex.message ?: "Invalid resource state", request)
    }

    // ------------------------------------------------------------
    // 5. SECURITY & AUTHENTICATION EXCEPTIONS
    // ------------------------------------------------------------
    @ExceptionHandler(UnauthenticatedException::class)
    fun handleUnauthenticated(ex: UnauthenticatedException, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.warn("Unauthenticated access attempt on [${request.method}] ${request.requestURI}: ${ex.message}")
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.message ?: "You must be logged in", request)
    }

    @ExceptionHandler(
        UnauthorizedException::class,
        org.springframework.security.access.AccessDeniedException::class // Catches Spring Security annotations
    )
    fun handleUnauthorized(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.warn("Forbidden access attempt on [${request.method}] ${request.requestURI}: ${ex.message}")
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.message ?: "You do not have permission to perform this action", request)
    }

    // ------------------------------------------------------------
    // 6. FALLBACK FOR EVERYTHING ELSE
    // ------------------------------------------------------------
    @ExceptionHandler(Exception::class)
    fun handleAll(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiError> {
        // Logged as an ERROR with the full stack trace because this caught a bug we didn't anticipate
        log.error("Unexpected server error occurred at [${request.method}] ${request.requestURI}", ex)
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred.", request)
    }

    // --- Helper Method ---
    private fun buildErrorResponse(status: HttpStatus, message: String, request: HttpServletRequest): ResponseEntity<ApiError> {
        val apiError = ApiError(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.requestURI
        )
        return ResponseEntity.status(status).body(apiError)
    }
}