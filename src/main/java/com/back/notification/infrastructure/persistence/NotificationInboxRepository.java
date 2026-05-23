package com.back.notification.infrastructure.persistence;

import com.back.notification.domain.NotificationInbox;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationInboxRepository extends JpaRepository<NotificationInbox, Long> {

    Optional<NotificationInbox> findByNotificationDeliveryId(Long notificationDeliveryId);

    Optional<NotificationInbox> findByNotificationDeliveryNotificationRequestId(Long notificationRequestId);

    Page<NotificationInbox> findByRecipientId(String recipientId, Pageable pageable);

    Page<NotificationInbox> findByRecipientIdAndReadAtIsNull(String recipientId, Pageable pageable);

    Page<NotificationInbox> findByRecipientIdAndReadAtIsNotNull(String recipientId, Pageable pageable);

    @Query(
            value = """
                    select i
                    from NotificationInbox i
                    join fetch i.notificationDelivery d
                    join fetch d.notificationRequest r
                    where i.recipientId = :recipientId
                      and i.visibleAt <= :now
                    """,
            countQuery = """
                    select count(i)
                    from NotificationInbox i
                    where i.recipientId = :recipientId
                      and i.visibleAt <= :now
                    """
    )
    Page<NotificationInbox> findVisibleByRecipientId(
            @Param("recipientId") String recipientId,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query(
            value = """
                    select i
                    from NotificationInbox i
                    join fetch i.notificationDelivery d
                    join fetch d.notificationRequest r
                    where i.recipientId = :recipientId
                      and i.visibleAt <= :now
                      and i.readAt is not null
                    """,
            countQuery = """
                    select count(i)
                    from NotificationInbox i
                    where i.recipientId = :recipientId
                      and i.visibleAt <= :now
                      and i.readAt is not null
                    """
    )
    Page<NotificationInbox> findVisibleReadByRecipientId(
            @Param("recipientId") String recipientId,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query(
            value = """
                    select i
                    from NotificationInbox i
                    join fetch i.notificationDelivery d
                    join fetch d.notificationRequest r
                    where i.recipientId = :recipientId
                      and i.visibleAt <= :now
                      and i.readAt is null
                    """,
            countQuery = """
                    select count(i)
                    from NotificationInbox i
                    where i.recipientId = :recipientId
                      and i.visibleAt <= :now
                      and i.readAt is null
                    """
    )
    Page<NotificationInbox> findVisibleUnreadByRecipientId(
            @Param("recipientId") String recipientId,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationInbox i
               set i.readAt = :readAt,
                   i.updatedAt = :readAt
             where i.recipientId = :recipientId
               and i.id in :inboxIds
               and i.readAt is null
               and i.visibleAt <= :readAt
            """)
    int markVisibleNotificationsRead(
            @Param("recipientId") String recipientId,
            @Param("inboxIds") Collection<Long> inboxIds,
            @Param("readAt") LocalDateTime readAt
    );
}
