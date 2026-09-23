package com.artifactalley.api;

import com.artifactalley.artifact.ArtifactOperationException;
import com.artifactalley.bid.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice(basePackages = "com.artifactalley.api")
public class RestApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(RestApiExceptionHandler.class);

    @ExceptionHandler(ApiAccessException.class)
    public ResponseEntity<ApiErrorResponse> access(ApiAccessException exception, HttpServletRequest request) {
        HttpStatus status = exception.isAuthenticated() ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
        return response(status, status == HttpStatus.UNAUTHORIZED ? "AUTHENTICATION_REQUIRED" : "INSUFFICIENT_ROLE",
                exception.getMessage(), request, List.of());
    }

    @ExceptionHandler({ArtifactNotFoundException.class, BidderNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> missing(RuntimeException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", safe(exception, "The requested resource was not found."), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiErrorResponse.FieldError(error.getField(), error.getDefaultMessage())).toList();
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "One or more fields are invalid.", request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> constraints(ConstraintViolationException exception, HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fields = exception.getConstraintViolations().stream()
                .map(error -> new ApiErrorResponse.FieldError(error.getPropertyPath().toString(), error.getMessage())).toList();
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "One or more request parameters are invalid.", request, fields);
    }

    @ExceptionHandler(com.artifactalley.artifact.ArtifactSearchValidationException.class)
    public ResponseEntity<ApiErrorResponse> searchValidation(
            com.artifactalley.artifact.ArtifactSearchValidationException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_SEARCH_FILTER", "One or more discovery filters are invalid.",
                request, List.of(new ApiErrorResponse.FieldError(exception.getField(), exception.getMessage())));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> malformed(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request could not be parsed.", request, List.of());
    }

    @ExceptionHandler(BidBelowMinimumException.class)
    public ResponseEntity<ApiErrorResponse> belowMinimum(BidBelowMinimumException exception, HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "BID_BELOW_MINIMUM", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(BidNotAllowedException.class)
    public ResponseEntity<ApiErrorResponse> bidNotAllowed(BidNotAllowedException exception, HttpServletRequest request) {
        boolean conflict = exception.getMessage().contains("not live") || exception.getMessage().contains("closed");
        return response(conflict ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_ENTITY,
                conflict ? "AUCTION_CONFLICT" : "BID_NOT_ALLOWED", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ArtifactOperationException.class)
    public ResponseEntity<ApiErrorResponse> artifactOperation(ArtifactOperationException exception, HttpServletRequest request) {
        boolean missing = exception.getMessage().toLowerCase().contains("not found");
        return response(missing ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT,
                missing ? "RESOURCE_NOT_FOUND" : "LISTING_CONFLICT", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiErrorResponse> concurrency(ConcurrencyFailureException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", "The resource changed; reload and try again.", request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> domain(IllegalArgumentException exception, HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "DOMAIN_VALIDATION", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.error("Unexpected REST error requestId={} path={}", requestId, request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "The request could not be completed.",
                request, List.of(), requestId);
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message,
                                                       HttpServletRequest request, List<ApiErrorResponse.FieldError> fields) {
        return response(status, code, message, request, fields, UUID.randomUUID().toString());
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message,
                                                       HttpServletRequest request, List<ApiErrorResponse.FieldError> fields,
                                                       String requestId) {
        return ResponseEntity.status(status).header("X-Request-ID", requestId).body(new ApiErrorResponse(
                OffsetDateTime.now(), status.value(), code, message, request.getRequestURI(), requestId, fields));
    }

    private String safe(RuntimeException exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().isBlank() ? fallback : exception.getMessage();
    }
}
