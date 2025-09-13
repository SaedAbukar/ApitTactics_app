package org.sportstechsolutions.apitacticsapp.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

// Standard API error response
data class ApiError(
    val status: Int,
    val error: String,
    val message: String,
    val fieldErrors: List<FieldValidationError>? = null
)

data class FieldValidationError(
    val field: String,
    val rejectedValue: Any?,
    val message: String
)

@ControllerAdvice
class GlobalExceptionHandler {

    // Validation errors from @Valid
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val errors = ex.bindingResult.allErrors.mapNotNull { error ->
            if (error is FieldError) {
                FieldValidationError(error.field, error.rejectedValue, error.defaultMessage ?: "")
            } else null
        }

        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = "Validation failed for one or more fields",
            fieldErrors = errors
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError)
    }

    // Resource not found
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<ApiError> {
        val apiError = ApiError(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "Resource not found"
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError)
    }

    // Unauthorized access
    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<ApiError> {
        val apiError = ApiError(
            status = HttpStatus.FORBIDDEN.value(),
            error = "Forbidden",
            message = ex.message ?: "You do not have permission to perform this action"
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiError)
    }

    // Fallback for all uncaught exceptions
    @ExceptionHandler(Exception::class)
    fun handleAll(ex: Exception): ResponseEntity<ApiError> {
        val apiError = ApiError(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = ex.message ?: "An unexpected error occurred"
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError)
    }
}
