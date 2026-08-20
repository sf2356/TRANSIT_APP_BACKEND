package com.transit.platform.common;

import org.springframework.http.HttpStatus;

/** Exception métier générique, porteuse d'un ErrorCode et d'un statut HTTP explicites. */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus httpStatus;

    public BusinessException(ErrorCode code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static BusinessException notFound(ErrorCode code, String message) {
        return new BusinessException(code, message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(ErrorCode.FORBIDDEN, message, HttpStatus.FORBIDDEN);
    }

    public static BusinessException conflict(ErrorCode code, String message) {
        return new BusinessException(code, message, HttpStatus.CONFLICT);
    }

    public static BusinessException badRequest(ErrorCode code, String message) {
        return new BusinessException(code, message, HttpStatus.BAD_REQUEST);
    }

    public static BusinessException unprocessable(ErrorCode code, String message) {
        return new BusinessException(code, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
