package com.back.notification.service;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.notification.domain.NotificationDelivery;
import com.back.notification.domain.NotificationManualRetry;
import com.back.notification.domain.NotificationRequest;
import com.back.notification.enums.DeliveryStatus;
import com.back.notification.infrastructure.persistence.NotificationDeliveryRepository;
import com.back.notification.infrastructure.persistence.NotificationManualRetryRepository;
import com.back.notification.infrastructure.persistence.NotificationRequestRepository;
import com.back.notification.web.dto.ManualRetryDeliveryResponse;
import com.back.notification.web.dto.ManualRetryRequest;
import com.back.notification.web.dto.ManualRetryResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationManualRetryService {

    private final NotificationRequestRepository notificationRequestRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationManualRetryRepository notificationManualRetryRepository;
    private final Clock clock;

    public NotificationManualRetryService(
            NotificationRequestRepository notificationRequestRepository,
            NotificationDeliveryRepository notificationDeliveryRepository,
            NotificationManualRetryRepository notificationManualRetryRepository,
            Clock clock
    ) {
        this.notificationRequestRepository = notificationRequestRepository;
        this.notificationDeliveryRepository = notificationDeliveryRepository;
        this.notificationManualRetryRepository = notificationManualRetryRepository;
        this.clock = clock;
    }

    @Transactional
    public ManualRetryResponse retryFailedNotification(
            Long notificationId,
            String requestedBy,
            ManualRetryRequest request
    ) {
        validate(requestedBy);

        NotificationRequest notificationRequest = notificationRequestRepository.findById(notificationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.NOTIFICATION_NOT_FOUND));
        List<NotificationDelivery> deliveries = notificationDeliveryRepository
                .findByNotificationRequestIdForUpdate(notificationRequest.getId());
        List<NotificationDelivery> failedDeliveries = deliveries.stream()
                .filter(delivery -> delivery.getStatus() == DeliveryStatus.FAILED)
                .toList();
        if (failedDeliveries.isEmpty()) {
            throw new ServiceException(ErrorCode.NOTIFICATION_MANUAL_RETRY_NOT_ALLOWED);
        }

        LocalDateTime requestedAt = LocalDateTime.now(clock);
        String reason = request == null ? null : request.reason();
        List<ManualRetryDeliveryResponse> retriedDeliveries = failedDeliveries.stream()
                .map(delivery -> retryDelivery(delivery, requestedBy, reason, requestedAt))
                .toList();

        return new ManualRetryResponse(
                notificationRequest.getId(),
                requestedBy,
                reason,
                requestedAt,
                retriedDeliveries.size(),
                retriedDeliveries
        );
    }

    private ManualRetryDeliveryResponse retryDelivery(
            NotificationDelivery delivery,
            String requestedBy,
            String reason,
            LocalDateTime requestedAt
    ) {
        int nextRetryCycle = delivery.getRetryCycle() + 1;
        NotificationManualRetry manualRetry = NotificationManualRetry.record(
                delivery,
                nextRetryCycle,
                requestedBy,
                reason,
                requestedAt
        );
        notificationManualRetryRepository.save(manualRetry);
        delivery.markManualRetryScheduled(requestedAt);

        return new ManualRetryDeliveryResponse(
                delivery.getId(),
                delivery.getChannel(),
                delivery.getStatus(),
                delivery.getRetryCycle(),
                delivery.getAttemptCount(),
                delivery.getManualRetryCount()
        );
    }

    private void validate(String requestedBy) {
        if (requestedBy == null || requestedBy.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
