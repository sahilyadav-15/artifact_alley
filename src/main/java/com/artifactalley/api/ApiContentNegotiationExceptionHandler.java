package com.artifactalley.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Handler selection can fail before Spring identifies an API controller, so negotiation errors are global. */
@RestControllerAdvice
public class ApiContentNegotiationExceptionHandler {
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> unsupported(HttpMediaTypeNotSupportedException exception,
                                                         HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Use application/json or application/xml.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiErrorResponse> unacceptable(HttpMediaTypeNotAcceptableException exception,
                                                          HttpServletRequest request) {
        return response(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE",
                "Only JSON and XML representations are available.", request);
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message,
                                                       HttpServletRequest request) {
        String requestId = UUID.randomUUID().toString();
        return ResponseEntity.status(status).header("X-Request-ID", requestId).body(new ApiErrorResponse(
                OffsetDateTime.now(), status.value(), code, message, request.getRequestURI(), requestId, List.of()));
    }
}
