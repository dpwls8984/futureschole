package com.back.notification.web.dto;

import com.back.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "사용자 인앱 알림 항목 응답")
public record InboxNotificationResponse(
        @Schema(description = "인앱 알림 ID", example = "1")
        Long inboxId,

        @Schema(description = "알림 요청 ID", example = "1")
        Long notificationId,

        @Schema(description = "인앱 채널 발송 작업 ID", example = "1")
        Long deliveryId,

        @Schema(description = "알림 타입", example = "COURSE_STARTING_TOMORROW")
        NotificationType notificationType,

        @Schema(description = "원천 이벤트 ID", example = "course-start-3001-20260523")
        String eventId,

        @Schema(description = "알림 제목", example = "강의 시작 알림")
        String title,

        @Schema(description = "알림 내용", example = "내일 수강 예정인 강의가 시작됩니다.")
        String message,

        @Schema(description = "알림 메시지 생성에 사용된 참조 데이터")
        Map<String, Object> referenceData,

        @Schema(description = "읽음 여부", example = "false")
        boolean read,

        @Schema(description = "사용자 알림함 노출 시각", example = "2026-05-22T19:30:00")
        LocalDateTime visibleAt,

        @Schema(description = "읽음 처리 시각. 읽지 않았으면 null", nullable = true, example = "2026-05-22T20:00:00")
        LocalDateTime readAt,

        @Schema(description = "인앱 알림 생성 시각", example = "2026-05-22T19:30:00")
        LocalDateTime createdAt
) {
}
