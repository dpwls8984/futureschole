package com.back.notification.infrastructure.persistence;

import com.back.notification.domain.NotificationAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, Long> {

    @Query("""
            select a
            from NotificationAttempt a
            where a.notificationDelivery.id = :notificationDeliveryId
            order by a.retryCycle asc, a.attemptNo asc
            """)
    List<NotificationAttempt> findByNotificationDeliveryIdOrderByAttemptNoAsc(
            @Param("notificationDeliveryId") Long notificationDeliveryId
    );
}
