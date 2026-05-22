package com.back.notification.infrastructure.persistence;

import com.back.notification.domain.NotificationInbox;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationInboxRepository extends JpaRepository<NotificationInbox, Long> {

    Optional<NotificationInbox> findByNotificationDeliveryId(Long notificationDeliveryId);

    Optional<NotificationInbox> findByNotificationDeliveryNotificationRequestId(Long notificationRequestId);

    Page<NotificationInbox> findByRecipientId(String recipientId, Pageable pageable);

    Page<NotificationInbox> findByRecipientIdAndReadAtIsNull(String recipientId, Pageable pageable);

    Page<NotificationInbox> findByRecipientIdAndReadAtIsNotNull(String recipientId, Pageable pageable);
}
