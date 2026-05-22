package com.back.notification.worker;

import com.back.notification.service.NotificationRecoveryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "notification.worker", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationRecoveryWorker {

    private final NotificationRecoveryService notificationRecoveryService;

    public NotificationRecoveryWorker(NotificationRecoveryService notificationRecoveryService) {
        this.notificationRecoveryService = notificationRecoveryService;
    }

    @Scheduled(fixedDelayString = "${notification.worker.recovery-fixed-delay}")
    public void recoverStuckDeliveries() {
        notificationRecoveryService.recoverStuckDeliveries();
    }
}
