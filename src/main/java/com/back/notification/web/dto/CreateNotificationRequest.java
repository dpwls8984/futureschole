package com.back.notification.web.dto;

import com.back.notification.enums.DispatchChannel;
import com.back.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Schema(description = "알림 발송 요청 등록 요청")
public record CreateNotificationRequest(
        @Schema(description = "알림 수신자 ID", example = "user-1001")
        @NotBlank
        String recipientId,

        @Schema(description = "알림 타입", example = "PAYMENT_CONFIRMED")
        @NotNull
        NotificationType notificationType,

        @Schema(description = "중복 방지 기준이 되는 원천 이벤트 ID", example = "payment-20260522-0001")
        @NotBlank
        String eventId,

        @Schema(description = "요청 채널 정책", example = "EMAIL")
        @NotNull
        DispatchChannel channel,

        @Schema(description = "알림 메시지 생성에 필요한 참조 데이터", example = "{\"paymentId\":\"pay-9001\",\"courseId\":\"course-3001\"}")
        Map<String, Object> referenceData
) {
}
