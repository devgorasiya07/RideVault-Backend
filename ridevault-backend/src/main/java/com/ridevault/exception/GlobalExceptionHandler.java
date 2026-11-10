package com.ridevault.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler for the entire REST API.
 *
 * Resume: "@Valid DTOs, streamlining safe client integration and rejecting
 *          malformed API requests"
 *
 * Every error response follows a consistent JSON structure:
 * {
 *   "timestamp": "...",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "...",
 *   "fieldErrors": { "email": "must be a valid email", ... }
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========================================================================
    //  @Valid DTO Validation Failures
    //  Catches MethodArgumentNotValidException thrown when @Valid fails
    // ========================================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = buildErrorBody(
                HttpStatus.BAD_REQUEST,
                "Validation failed: request contains invalid or missing fields"
        );
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    // ========================================================================
    //  Resource Not Found (404)
    // ========================================================================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ========================================================================
    //  Ride Already Claimed — Optimistic Locking Conflict (409)
    //  Resume: "eradicate double-booking race conditions under load"
    // ========================================================================
    @ExceptionHandler(RideAlreadyClaimedException.class)
    public ResponseEntity<Map<String, Object>> handleRideConflict(
            RideAlreadyClaimedException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ========================================================================
    //  Invalid Ride State Transition (400)
    //  Resume: "Orchestrated ride state machine workflows"
    // ========================================================================
    @ExceptionHandler(InvalidRideStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidState(
            InvalidRideStateException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ========================================================================
    //  Insufficient Wallet Funds (400)
    // ========================================================================
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientFunds(
            InsufficientFundsException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ========================================================================
    //  Bad Credentials (401)
    // ========================================================================
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex) {
        String msg = (ex.getMessage() != null && !ex.getMessage().isBlank() && !ex.getMessage().equals("Bad credentials"))
                ? ex.getMessage()
                : "Invalid credentials";
        return buildResponse(HttpStatus.UNAUTHORIZED, msg);
    }

    // ========================================================================
    //  Illegal Argument & Validation Logic (400)
    // ========================================================================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ========================================================================
    //  Illegal State / Conflict (409)
    // ========================================================================
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ========================================================================
    //  Database Constraint Violations (409)
    // ========================================================================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        return buildResponse(HttpStatus.CONFLICT,
                "Conflict with existing database record or constraint violation.");
    }

    // ========================================================================
    //  Malformed Requests & Missing Params (400)
    // ========================================================================
    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, Object>> handleMalformedRequest(Exception ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request payload or invalid parameter type.");
    }

    // ========================================================================
    //  Access Denied (403)
    // ========================================================================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex) {
        String msg = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "You do not have permission to access this resource";
        return buildResponse(HttpStatus.FORBIDDEN, msg);
    }

    // ========================================================================
    //  Catch-All for Unhandled Exceptions (500)
    // ========================================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred: " + ex.getMessage());
    }

    // ========================================================================
    //  Helper Methods
    // ========================================================================
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String message) {
        return ResponseEntity.status(status).body(buildErrorBody(status, message));
    }

    private Map<String, Object> buildErrorBody(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}
