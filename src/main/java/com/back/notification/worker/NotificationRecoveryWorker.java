package com.back.notification.worker;

import com.back.notification.service.NotificationRecoveryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationRecoveryWorker {

    private final NotificationRecoveryService notificationRecoveryService;

    public NotificationRecoveryWorker(NotificationRecoveryService notificationRecoveryService) {
        this.notificationRecoveryService = notificationRecoveryService;
    }

    @Scheduled(fixedDelayString = "${notification.worker.recovery-fixed-delay}")
    public void recoverStuckDeliveries() {
        // Recovery logic will live in the service; the worker remains a scheduler entry point.
    }
}
