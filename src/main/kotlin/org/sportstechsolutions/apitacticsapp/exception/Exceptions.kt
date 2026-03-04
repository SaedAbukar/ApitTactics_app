package org.sportstechsolutions.apitacticsapp.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class ResourceNotFoundException(message: String) : RuntimeException(message)

// 403: User is logged in, but does not have permission (e.g., trying to edit someone else's session)
@ResponseStatus(HttpStatus.FORBIDDEN)
class UnauthorizedException(message: String) : RuntimeException(message)

// 401: User is completely anonymous and needs to log in
@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnauthenticatedException(message: String) : RuntimeException(message)

// 409: Business rule violations (like deleting a session in use)
@ResponseStatus(HttpStatus.CONFLICT)
class ConflictException(message: String) : RuntimeException(message)