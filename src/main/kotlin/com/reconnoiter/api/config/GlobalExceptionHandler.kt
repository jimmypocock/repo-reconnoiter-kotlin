package com.reconnoiter.api.config

import jakarta.servlet.http.HttpServletRequest

import org.slf4j.LoggerFactory

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

import java.time.LocalDateTime

import com.reconnoiter.api.controller.RepositoryNotFoundException
import com.reconnoiter.api.controller.InvalidSearchQueryException
import com.reconnoiter.api.controller.ComparisonNotFoundException

/**
 * Global Exception Handler
 *
 * Centralized exception handling for all REST controllers.
 * Returns consistent JSON error responses following professional API standards.
 */
@RestControllerAdvice
class GlobalExceptionHandler(
    @Value("\${spring.profiles.active:dev}") private val activeProfile: String
) {

    //--------------------------------------
    // CONSTANTS
    //--------------------------------------

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    private val isDevelopment = activeProfile.contains("dev")

    //--------------------------------------
    // PUBLIC INSTANCE METHODS
    //--------------------------------------

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Access denied: ${ex.message} - Path: ${request.requestURI}")
        val errorResponse = ErrorResponse(
            error = "Forbidden",
            message = ex.message ?: "Access denied",
            path = request.requestURI,
            status = HttpStatus.FORBIDDEN.value(),
            timestamp = LocalDateTime.now()
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(
        ex: BadCredentialsException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Bad credentials: ${ex.message} - Path: ${request.requestURI}")
        val errorResponse = ErrorResponse(
            error = "Unauthorized",
            message = "Invalid authentication credentials",
            path = request.requestURI,
            status = HttpStatus.UNAUTHORIZED.value(),
            timestamp = LocalDateTime.now()
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred - Path: ${request.requestURI}", ex)

        val message = if (isDevelopment) {
            "${ex.javaClass.simpleName}: ${ex.message}"
        } else {
            "An unexpected error occurred. Please try again later."
        }

        val details = if (isDevelopment) {
            ex.stackTrace.take(5).map { it.toString() }
        } else {
            null
        }

        val errorResponse = ErrorResponse(
            error = "Internal Server Error",
            message = message,
            path = request.requestURI,
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            timestamp = LocalDateTime.now(),
            details = details
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Malformed JSON request: ${ex.message} - Path: ${request.requestURI}")
        val errorResponse = ErrorResponse(
            error = "Bad Request",
            message = "Malformed JSON request body",
            path = request.requestURI,
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        logger.warn("Validation failed: $fieldErrors - Path: ${request.requestURI}")
        val errorResponse = ErrorResponse(
            details = fieldErrors,
            error = "Bad Request",
            message = "Validation failed",
            path = request.requestURI,
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Missing parameter: ${ex.parameterName} - Path: ${request.requestURI}")
        val errorResponse = ErrorResponse(
            error = "Bad Request",
            message = "Missing required parameter: ${ex.parameterName}",
            path = request.requestURI,
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(ComparisonNotFoundException::class)
    fun handleComparisonNotFound(
        ex: ComparisonNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Comparison not found: ${ex.message} - Path: ${request.requestURI}")
        val errorResponse = ErrorResponse(
            error = "Not Found",
            message = ex.message ?: "Comparison not found",
            path = request.requestURI,
            status = HttpStatus.NOT_FOUND.value(),
            timestamp = LocalDateTime.now()
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(InvalidSearchQueryException::class)
    fun handleInvalidSearchQuery(
        ex: InvalidSearchQueryException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid search query: ${ex.message} - Path: ${request.requestURI}")
        val errorResponse = ErrorResponse(
            error = "Bad Request",
            message = ex.message ?: "Invalid search query",
            path = request.requestURI,
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now()
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(RepositoryNotFoundException::class)
    fun handleRepositoryNotFound(
        ex: RepositoryNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Repository not found: ${ex.message} - Path: ${request.requestURI}")
        val errorResponse = ErrorResponse(
            error = "Not Found",
            message = ex.message ?: "Repository not found",
            path = request.requestURI,
            status = HttpStatus.NOT_FOUND.value(),
            timestamp = LocalDateTime.now()
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }
}

/**
 * Standard Error Response
 *
 * Follows industry best practices for API error responses.
 * Includes timestamp, path, status, and optional validation details.
 */
data class ErrorResponse(
    val error: String,
    val message: String,
    val path: String,
    val status: Int,
    val timestamp: LocalDateTime,
    val details: List<String>? = null
)
