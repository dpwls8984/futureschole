package com.back.notification.web.dto;

import com.back.notification.enums.DeliveryStatus;
import com.back.notification.enums.DispatchChannel;
import com.back.notification.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record NotificationDetailResponse(
        Long notificationId,
        String recipientId,
        NotificationType notificationType,
        String eventId,
        DispatchChannel requestedChannel,
        Map<String, Object> referenceData,
        DeliveryStatus status,
        List<DeliveryResponse> deliveries,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
