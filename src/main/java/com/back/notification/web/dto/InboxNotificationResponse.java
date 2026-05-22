package com.back.notification.web.dto;

import com.back.notification.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.Map;

public record InboxNotificationResponse(
        Long inboxId,
        Long notificationId,
        NotificationType notificationType,
        String eventId,
        String title,
        String message,
        Map<String, Object> referenceData,
        LocalDateTime visibleAt,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
}
