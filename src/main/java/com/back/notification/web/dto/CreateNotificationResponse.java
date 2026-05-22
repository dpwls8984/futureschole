package com.back.notification.web.dto;

import com.back.notification.enums.DeliveryStatus;
import com.back.notification.enums.DispatchChannel;
import com.back.notification.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.List;

public record CreateNotificationResponse(
        Long notificationId,
        String recipientId,
        NotificationType notificationType,
        String eventId,
        DispatchChannel requestedChannel,
        DeliveryStatus status,
        List<DeliveryResponse> deliveries,
        boolean duplicated,
        LocalDateTime createdAt
) {
}
