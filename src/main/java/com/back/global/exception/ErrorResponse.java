package com.back.global.exception;

public record ErrorResponse(ErrorBody error) {

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(
                new ErrorBody(
                        errorCode.name(),
                        String.valueOf(errorCode.getStatus().value()),
                        errorCode.getMessage()
                )
        );
    }

    public record ErrorBody(
            String code,
            String status,
            String message
    ) {
    }
}
