package com.back.notification.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "인앱 알림 읽음 처리 응답")
public record MarkReadResponse(
        @Schema(description = "알림 요청 ID", example = "1")
        Long notificationId,

        @Schema(description = "알림 수신자 ID", example = "user-1001")
        String recipientId,

        @Schema(description = "읽음 여부", example = "true")
        boolean read,

        @Schema(description = "읽음 처리 시각", example = "2026-05-22T20:00:00")
        LocalDateTime readAt
) {
}
