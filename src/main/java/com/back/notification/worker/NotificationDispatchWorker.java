package com.back.notification.worker;

import com.back.notification.service.NotificationWorkerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationDispatchWorker {

    private final NotificationWorkerService notificationWorkerService;

    public NotificationDispatchWorker(NotificationWorkerService notificationWorkerService) {
        this.notificationWorkerService = notificationWorkerService;
    }

    @Scheduled(fixedDelayString = "${notification.worker.dispatch-fixed-delay}")
    public void dispatchDueDeliveries() {
        // Business logic will live in the service; the worker remains a scheduler entry point.
    }
}
