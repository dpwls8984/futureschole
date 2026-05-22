package com.back.notification.infrastructure.persistence;

import com.back.notification.domain.NotificationAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, Long> {

    List<NotificationAttempt> findByNotificationDeliveryIdOrderByAttemptNoAsc(Long notificationDeliveryId);
}
