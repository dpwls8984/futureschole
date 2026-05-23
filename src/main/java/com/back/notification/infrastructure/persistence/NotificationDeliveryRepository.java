package com.back.notification.infrastructure.persistence;

import com.back.notification.domain.NotificationDelivery;
import com.back.notification.enums.DeliveryStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    List<NotificationDelivery> findByNotificationRequestId(Long notificationRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d
            from NotificationDelivery d
            where d.notificationRequest.id = :notificationRequestId
            order by d.id asc
            """)
    List<NotificationDelivery> findByNotificationRequestIdForUpdate(
            @Param("notificationRequestId") Long notificationRequestId
    );

    @Query("""
            select d
            from NotificationDelivery d
            join fetch d.notificationRequest
            where d.id = :deliveryId
            """)
    Optional<NotificationDelivery> findByIdWithRequest(@Param("deliveryId") Long deliveryId);

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationDelivery d
               set d.status = :pending,
                   d.lockOwner = null,
                   d.lockedUntil = null,
                   d.availableAt = :now,
                   d.updatedAt = :now
             where d.status = :processing
               and d.lockedUntil < :now
            """)
    int restoreExpiredProcessingDeliveries(
            @Param("processing") DeliveryStatus processing,
            @Param("pending") DeliveryStatus pending,
            @Param("now") LocalDateTime now
    );
}
