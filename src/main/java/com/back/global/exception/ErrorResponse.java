package com.back.global.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 에러 응답")
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

    @Schema(description = "에러 상세")
    public record ErrorBody(
            @Schema(description = "에러 코드 이름", example = "NOTIFICATION_CHANNEL_CONFLICT")
            String code,

            @Schema(description = "HTTP 상태 코드", example = "409")
            String status,

            @Schema(description = "에러 메시지", example = "동일 알림 요청이 다른 채널로 이미 존재합니다.")
            String message
    ) {
    }
}
