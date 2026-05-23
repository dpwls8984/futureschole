package com.back.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    BAD_REQUEST("001", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_INPUT_VALUE("004", HttpStatus.BAD_REQUEST, "유효하지 않은 입력 값입니다."),
    INTERNAL_SERVER_ERROR("003", HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 오류가 발생했습니다."),

    NOTIFICATION_NOT_FOUND("1001", HttpStatus.NOT_FOUND, "알림 요청을 찾을 수 없습니다."),
    DELIVERY_NOT_FOUND("1002", HttpStatus.NOT_FOUND, "알림 발송 작업을 찾을 수 없습니다."),
    NOTIFICATION_CHANNEL_CONFLICT("1003", HttpStatus.CONFLICT, "동일 알림 요청이 다른 채널로 이미 존재합니다."),
    UNSUPPORTED_NOTIFICATION_TYPE("1004", HttpStatus.BAD_REQUEST, "지원하지 않는 알림 타입입니다."),
    READ_NOT_SUPPORTED("1005", HttpStatus.BAD_REQUEST, "읽음 처리는 인앱 알림에만 지원됩니다."),
    IN_APP_NOTIFICATION_NOT_VISIBLE("1006", HttpStatus.CONFLICT, "인앱 알림이 아직 사용자 알림함에 노출되지 않았습니다."),
    NOTIFICATION_MANUAL_RETRY_NOT_ALLOWED("1007", HttpStatus.CONFLICT, "수동 재시도 가능한 최종 실패 알림이 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
