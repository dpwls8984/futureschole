package com.back.notification.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "운영자 수동 재시도 응답")
public record ManualRetryResponse(
        @Schema(description = "알림 요청 ID", example = "1")
        Long notificationId,

        @Schema(description = "수동 재시도 요청자", example = "admin-1")
        String requestedBy,

        @Schema(description = "수동 재시도 사유", example = "외부 이메일 서버 장애 복구 후 재시도합니다.")
        String reason,

        @Schema(description = "수동 재시도 처리 시각", example = "2026-05-23T11:00:00")
        LocalDateTime requestedAt,

        @Schema(description = "수동 재시도 대상 발송 작업 수", example = "1")
        int retriedDeliveryCount,

        @Schema(description = "수동 재시도 대상 발송 작업 목록")
        List<ManualRetryDeliveryResponse> deliveries
) {
}
