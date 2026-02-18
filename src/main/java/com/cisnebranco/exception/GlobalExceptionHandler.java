package com.cisnebranco.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(404, ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(401, ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError(422, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, "Validation failed", details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, "Malformed request body"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, "Invalid parameter: " + ex.getName()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Failed login attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(401, "Invalid username or password"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiError(413, "File size exceeds maximum allowed limit"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(403, "Access denied"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        String rootMsg = ex.getMostSpecificCause().getMessage();
        log.warn("Data integrity violation: {}", rootMsg);

        if (rootMsg != null && rootMsg.contains("Appointment conflicts")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError(409, "Appointment conflicts with an existing appointment for this groomer"));
        }
        if (rootMsg != null && rootMsg.contains("unique constraint") || rootMsg != null && rootMsg.contains("duplicate key")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError(409, "A record with this data already exists"));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(409, "Data integrity violation: operation conflicts with existing data"));
    }

    // Covers PessimisticLockingFailureException, CannotAcquireLockException,
    // DeadlockLoserDataAccessException, CannotSerializeTransactionException, and
    // OptimisticLockingFailureException — all concurrency conflicts should prompt a client retry.
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiError> handleConcurrencyConflict(ConcurrencyFailureException ex) {
        log.warn("Concurrency conflict — returning 409", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(409, "Este registro está sendo alterado por outro usuário. Aguarde um momento e tente novamente."));
    }

    @ExceptionHandler(QueryTimeoutException.class)
    public ResponseEntity<ApiError> handleLockTimeout(QueryTimeoutException ex) {
        log.warn("Lock or query timeout — returning 409", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(409, "Não foi possível obter acesso exclusivo ao registro. Aguarde um momento e tente novamente."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(500, "Internal server error"));
    }
}
