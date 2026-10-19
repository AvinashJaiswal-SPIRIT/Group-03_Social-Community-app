package com.smartCommunity.SmartCommunity.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns every error into the SAME JSON shape, so the frontend only needs one error handler:
 *
 *   { "status": 409, "message": "You are already a member of this community", "errors": null }
 *
 * "errors" is only filled for validation problems: { "name": "size must be between 3 and 50" }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ApiError(int status, String message, Map<String, String> errors) {
    }

    // Our services throw ResponseStatusException (404, 409, 403 ...) - return its message as JSON
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(new ApiError(ex.getStatusCode().value(), ex.getReason(), null));
    }

    // @Valid failed on a request body (blank name, too long, missing field ...)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        return ResponseEntity
                .badRequest()
                .body(new ApiError(400, "Validation failed", errors));
    }

    // Safety net for race conditions: two requests creating the same name / joining at the same moment
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDuplicate(DataIntegrityViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(409, "This already exists (duplicate name or membership)", null));
    }

    // Wrong password on login
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(401, "Invalid email or password", null));
    }
}
