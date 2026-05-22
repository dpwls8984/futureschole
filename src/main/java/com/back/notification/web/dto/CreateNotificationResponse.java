package com.back.notification.web.dto;

import com.back.notification.enums.DeliveryStatus;
import com.back.notification.enums.DispatchChannel;
import com.back.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "알림 발송 요청 등록 응답")
public record CreateNotificationResponse(
        @Schema(description = "알림 요청 ID", example = "1")
        Long notificationId,

        @Schema(description = "알림 수신자 ID", example = "user-1001")
        String recipientId,

        @Schema(description = "알림 타입", example = "PAYMENT_CONFIRMED")
        NotificationType notificationType,

        @Schema(description = "원천 이벤트 ID", example = "payment-20260522-0001")
        String eventId,

        @Schema(description = "요청 채널 정책", example = "EMAIL")
        DispatchChannel requestedChannel,

        @Schema(description = "알림 요청의 집계 상태", example = "PENDING")
        DeliveryStatus status,

        @Schema(description = "채널별 발송 작업 목록")
        List<DeliveryResponse> deliveries,

        @Schema(description = "이미 존재하던 동일 요청인지 여부", example = "false")
        boolean duplicated,

        @Schema(description = "요청 생성 시각", example = "2026-05-22T19:30:00")
        LocalDateTime createdAt
) {
}
