package com.back.notification.infrastructure.persistence;

import java.time.LocalDateTime;

public interface NotificationClaimRepository {

    int claimDelivery(
            Long deliveryId,
            String lockOwner,
            LocalDateTime lockedUntil,
            LocalDateTime now
    );
}
