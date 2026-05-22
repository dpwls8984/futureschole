package com.back.notification.service;

import com.back.notification.domain.NotificationDelivery;
import com.back.notification.enums.DeliveryStatus;
import com.back.notification.web.dto.DeliveryResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class NotificationResponseMapper {

    DeliveryResponse toDeliveryResponse(NotificationDelivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getChannel(),
                delivery.getStatus(),
                delivery.getAttemptCount(),
                delivery.getMaxAttempts(),
                delivery.getAvailableAt(),
                delivery.getLastFailureCode(),
                delivery.getLastFailureMessage(),
                delivery.getSucceededAt(),
                delivery.getFailedAt()
        );
    }

    List<DeliveryResponse> toDeliveryResponses(List<NotificationDelivery> deliveries) {
        return deliveries.stream()
                .sorted(Comparator.comparing(NotificationDelivery::getId))
                .map(this::toDeliveryResponse)
                .toList();
    }

    DeliveryStatus aggregateStatus(List<NotificationDelivery> deliveries) {
        if (deliveries.isEmpty()) {
            return DeliveryStatus.PENDING;
        }
        if (deliveries.stream().allMatch(delivery -> delivery.getStatus() == DeliveryStatus.SUCCEEDED)) {
            return DeliveryStatus.SUCCEEDED;
        }
        if (deliveries.stream().anyMatch(delivery -> delivery.getStatus() == DeliveryStatus.FAILED)) {
            return DeliveryStatus.FAILED;
        }
        if (deliveries.stream().anyMatch(delivery -> delivery.getStatus() == DeliveryStatus.PROCESSING)) {
            return DeliveryStatus.PROCESSING;
        }
        if (deliveries.stream().anyMatch(delivery -> delivery.getStatus() == DeliveryStatus.RETRY_WAITING)) {
            return DeliveryStatus.RETRY_WAITING;
        }
        return DeliveryStatus.PENDING;
    }
}
