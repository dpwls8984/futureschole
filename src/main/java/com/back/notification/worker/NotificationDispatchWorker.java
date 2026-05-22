package com.back.notification.worker;

import com.back.notification.service.NotificationWorkerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "notification.worker", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationDispatchWorker {

    private final NotificationWorkerService notificationWorkerService;

    public NotificationDispatchWorker(NotificationWorkerService notificationWorkerService) {
        this.notificationWorkerService = notificationWorkerService;
    }

    @Scheduled(fixedDelayString = "${notification.worker.dispatch-fixed-delay}")
    public void dispatchDueDeliveries() {
        notificationWorkerService.processDueDeliveries();
    }
}
