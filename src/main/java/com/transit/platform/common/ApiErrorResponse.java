package com.transit.platform.common;

import java.time.Instant;
import java.util.List;

/**
 * Structure d'erreur standardisée (Prompt 04 §7-8) — stable pour Angular ET Flutter.
 *
 * {
 *   "success": false,
 *   "error": { "code": "...", "message": "...", "details": [ { "field": "...", "message": "..." } ] },
 *   "timestamp": "...",
 *   "path": "..."
 * }
 */
public record ApiErrorResponse(boolean success, ErrorDetail error, Instant timestamp, String path) {

    public record ErrorDetail(String code, String message, List<FieldError> details) {}

    public record FieldError(String field, String message) {}

    public static ApiErrorResponse of(ErrorCode code, String message, String path) {
        return new ApiErrorResponse(false, new ErrorDetail(code.name(), message, null), Instant.now(), path);
    }

    public static ApiErrorResponse ofValidation(String message, List<FieldError> details, String path) {
        return new ApiErrorResponse(false, new ErrorDetail(ErrorCode.VALIDATION_ERROR.name(), message, details), Instant.now(), path);
    }
}
