package com.back.notification.infrastructure.persistence;

import com.back.notification.domain.NotificationRequest;
import com.back.notification.enums.NotificationType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, Long> {

    Optional<NotificationRequest> findByRecipientIdAndNotificationTypeAndEventId(
            String recipientId,
            NotificationType notificationType,
            String eventId
    );
}
