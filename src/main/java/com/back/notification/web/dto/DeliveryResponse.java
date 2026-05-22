package com.back.notification.web.dto;

import com.back.notification.enums.DeliveryChannel;
import com.back.notification.enums.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "채널별 알림 발송 작업 응답")
public record DeliveryResponse(
        @Schema(description = "발송 작업 ID", example = "1")
        Long deliveryId,

        @Schema(description = "실제 발송 채널", example = "EMAIL")
        DeliveryChannel channel,

        @Schema(description = "발송 작업 상태", example = "PENDING")
        DeliveryStatus status,

        @Schema(description = "현재까지 시도 횟수", example = "0")
        int attemptCount,

        @Schema(description = "최대 시도 횟수", example = "5")
        int maxAttempts,

        @Schema(description = "다음 처리 가능 시각", example = "2026-05-22T19:30:00")
        LocalDateTime availableAt,

        @Schema(description = "마지막 실패 코드", nullable = true, example = "EMAIL_SERVER_TIMEOUT")
        String lastFailureCode,

        @Schema(description = "마지막 실패 메시지", nullable = true, example = "Mock email server timeout.")
        String lastFailureMessage,

        @Schema(description = "성공 시각", nullable = true, example = "2026-05-22T19:31:00")
        LocalDateTime succeededAt,

        @Schema(description = "최종 실패 시각", nullable = true, example = "2026-05-22T19:35:00")
        LocalDateTime failedAt
) {
}
