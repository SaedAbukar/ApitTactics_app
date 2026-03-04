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

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // ------------------------------------------------------------
    // 1. SPRING VALIDATION ERRORS (@Valid DTO constraints)
    // ------------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ApiError> {
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
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request. Please check your syntax.", request)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val requiredType = ex.requiredType?.simpleName ?: "Unknown"
        val message = "Parameter '${ex.name}' should be of type $requiredType"
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParams(ex: MissingServletRequestParameterException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val message = "Required request parameter '${ex.parameterName}' is missing"
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: HttpServletRequest): ResponseEntity<ApiError> {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request parameters", request)
    }

    // ------------------------------------------------------------
    // 3. DATABASE EXCEPTIONS
    // ------------------------------------------------------------
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException, request: HttpServletRequest): ResponseEntity<ApiError> {
        // This catches things like Duplicate Emails or Foreign Key constraint failures
        logger.error("Database constraint violation", ex)
        return buildErrorResponse(HttpStatus.CONFLICT, "Database constraint violation. A record may already exist or is currently in use.", request)
    }

    // ------------------------------------------------------------
    // 4. CUSTOM DOMAIN EXCEPTIONS
    // ------------------------------------------------------------
    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException, request: HttpServletRequest): ResponseEntity<ApiError> {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.message ?: "Resource is currently in use", request)
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException, request: HttpServletRequest): ResponseEntity<ApiError> {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found", request)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException, request: HttpServletRequest): ResponseEntity<ApiError> {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.message ?: "Invalid resource state", request)
    }

    // ------------------------------------------------------------
    // 5. SECURITY & AUTHENTICATION EXCEPTIONS
    // ------------------------------------------------------------
    @ExceptionHandler(UnauthenticatedException::class)
    fun handleUnauthenticated(ex: UnauthenticatedException, request: HttpServletRequest): ResponseEntity<ApiError> {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.message ?: "You must be logged in", request)
    }

    @ExceptionHandler(
        UnauthorizedException::class,
        org.springframework.security.access.AccessDeniedException::class // Catches Spring Security annotations
    )
    fun handleUnauthorized(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiError> {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.message ?: "You do not have permission to perform this action", request)
    }

    // ------------------------------------------------------------
    // 6. FALLBACK FOR EVERYTHING ELSE
    // ------------------------------------------------------------
    @ExceptionHandler(Exception::class)
    fun handleAll(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiError> {
        logger.error("Unexpected error occurred at ${request.requestURI}", ex)
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