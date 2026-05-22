package com.back.notification.web.dto;

import java.util.List;

public record InboxNotificationListResponse(
        String recipientId,
        Boolean read,
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<InboxNotificationResponse> items
) {
}
