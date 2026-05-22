package com.back.notification.web.dto;

import com.back.notification.enums.DeliveryChannel;
import com.back.notification.enums.DeliveryStatus;
import java.time.LocalDateTime;

public record DeliveryResponse(
        Long deliveryId,
        DeliveryChannel channel,
        DeliveryStatus status,
        int attemptCount,
        int maxAttempts,
        LocalDateTime availableAt,
        String lastFailureCode,
        String lastFailureMessage,
        LocalDateTime succeededAt,
        LocalDateTime failedAt
) {
}
