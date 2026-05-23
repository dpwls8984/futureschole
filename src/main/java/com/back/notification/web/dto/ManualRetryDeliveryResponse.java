package com.back.notification.web.dto;

import com.back.notification.enums.DeliveryChannel;
import com.back.notification.enums.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수동 재시도 대상 발송 작업 응답")
public record ManualRetryDeliveryResponse(
        @Schema(description = "발송 작업 ID", example = "1")
        Long deliveryId,

        @Schema(description = "발송 채널", example = "EMAIL")
        DeliveryChannel channel,

        @Schema(description = "수동 재시도 후 상태", example = "PENDING")
        DeliveryStatus status,

        @Schema(description = "새 재시도 사이클 번호", example = "1")
        int retryCycle,

        @Schema(description = "초기화된 현재 사이클 시도 횟수", example = "0")
        int attemptCount,

        @Schema(description = "누적 수동 재시도 요청 횟수", example = "1")
        int manualRetryCount
) {
}
