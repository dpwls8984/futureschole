package com.back.notification.web.dto;

import com.back.notification.enums.DeliveryStatus;
import com.back.notification.enums.DispatchChannel;
import com.back.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "알림 요청 상태 상세 응답")
public record NotificationDetailResponse(
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

        @Schema(description = "알림 메시지 생성에 사용되는 참조 데이터")
        Map<String, Object> referenceData,

        @Schema(description = "알림 요청의 집계 상태", example = "PENDING")
        DeliveryStatus status,

        @Schema(description = "채널별 발송 작업 목록")
        List<DeliveryResponse> deliveries,

        @Schema(description = "요청 생성 시각", example = "2026-05-22T19:30:00")
        LocalDateTime createdAt,

        @Schema(description = "요청 수정 시각", example = "2026-05-22T19:30:00")
        LocalDateTime updatedAt
) {
}
