package com.back.notification.infrastructure.persistence;

import com.back.notification.domain.NotificationDelivery;
import com.back.notification.enums.DeliveryStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    List<NotificationDelivery> findByNotificationRequestId(Long notificationRequestId);

    @Query("""
            select d.id
            from NotificationDelivery d
            where d.status in :statuses
              and d.availableAt <= :now
              and (d.lockedUntil is null or d.lockedUntil < :now)
            order by d.availableAt asc, d.id asc
            """)
    List<Long> findDueDeliveryIds(
            @Param("statuses") Collection<DeliveryStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
