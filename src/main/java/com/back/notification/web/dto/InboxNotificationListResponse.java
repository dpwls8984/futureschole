package com.back.notification.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "사용자 인앱 알림 목록 응답")
public record InboxNotificationListResponse(
        @Schema(description = "조회 대상 사용자 ID", example = "user-1001")
        String recipientId,

        @Schema(description = "읽음 여부 필터. null이면 전체 조회", nullable = true, example = "false")
        Boolean read,

        @Schema(description = "현재 페이지 번호", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "20")
        int size,

        @Schema(description = "전체 항목 수", example = "2")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "1")
        int totalPages,

        @Schema(description = "인앱 알림 항목 목록")
        List<InboxNotificationResponse> items
) {
}
