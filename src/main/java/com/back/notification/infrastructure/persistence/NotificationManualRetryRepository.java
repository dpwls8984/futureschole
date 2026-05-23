package com.back.notification.infrastructure.persistence;

import com.back.notification.domain.NotificationManualRetry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationManualRetryRepository extends JpaRepository<NotificationManualRetry, Long> {

    List<NotificationManualRetry> findByNotificationDeliveryIdOrderByRetryCycleAsc(Long notificationDeliveryId);
}
