package com.back.notification.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "인앱 알림 읽음 처리 응답")
public record MarkReadResponse(
        @Schema(description = "알림 수신자 ID", example = "user-1001")
        String recipientId,

        @Schema(description = "읽음 처리를 요청한 고유 인앱 알림 수", example = "10")
        int requestedCount,

        @Schema(description = "이번 요청으로 실제 읽음 처리된 인앱 알림 수", example = "8")
        int markedCount,

        @Schema(description = "읽음 처리 시각", example = "2026-05-22T20:00:00")
        LocalDateTime readAt
) {
}
