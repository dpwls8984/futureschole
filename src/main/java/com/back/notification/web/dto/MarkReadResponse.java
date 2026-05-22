package com.back.notification.web.dto;

import java.time.LocalDateTime;

public record MarkReadResponse(
        Long notificationId,
        String recipientId,
        boolean read,
        LocalDateTime readAt
) {
}
