package com.back.notification.service;

import com.back.notification.enums.DeliveryStatus;
import com.back.notification.infrastructure.persistence.NotificationDeliveryRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationRecoveryService {

    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final Clock clock;

    public NotificationRecoveryService(
            NotificationDeliveryRepository notificationDeliveryRepository,
            Clock clock
    ) {
        this.notificationDeliveryRepository = notificationDeliveryRepository;
        this.clock = clock;
    }

    @Transactional
    public int recoverStuckDeliveries() {
        LocalDateTime now = LocalDateTime.now(clock);
        return notificationDeliveryRepository.restoreExpiredProcessingDeliveries(
                DeliveryStatus.PROCESSING,
                DeliveryStatus.PENDING,
                now
        );
    }
}
