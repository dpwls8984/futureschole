package com.back.notification.infrastructure.persistence;

import com.back.notification.enums.DeliveryStatus;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class NotificationClaimRepositoryImpl implements NotificationClaimRepository {

    private final EntityManager entityManager;

    public NotificationClaimRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public int claimDelivery(
            Long deliveryId,
            String lockOwner,
            LocalDateTime lockedUntil,
            LocalDateTime now
    ) {
        return entityManager.createQuery("""
                        update NotificationDelivery d
                           set d.status = :processing,
                               d.lockOwner = :lockOwner,
                               d.lockedUntil = :lockedUntil,
                               d.attemptCount = d.attemptCount + 1,
                               d.updatedAt = :now
                         where d.id = :deliveryId
                           and d.status in :claimableStatuses
                           and d.availableAt <= :now
                           and (d.lockedUntil is null or d.lockedUntil < :now)
                        """)
                .setParameter("processing", DeliveryStatus.PROCESSING)
                .setParameter("lockOwner", lockOwner)
                .setParameter("lockedUntil", lockedUntil)
                .setParameter("now", now)
                .setParameter("deliveryId", deliveryId)
                .setParameter("claimableStatuses", List.of(DeliveryStatus.PENDING, DeliveryStatus.RETRY_WAITING))
                .executeUpdate();
    }
}
