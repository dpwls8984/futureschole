package com.back.notification.domain;

import com.back.notification.enums.DeliveryStatus;
import com.back.notification.enums.FailureType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_manual_retries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_manual_retry_cycle",
                        columnNames = {"notification_delivery_id", "retry_cycle"}
                )
        }
)
public class NotificationManualRetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_delivery_id", nullable = false)
    private NotificationDelivery notificationDelivery;

    @Column(name = "retry_cycle", nullable = false)
    private int retryCycle;

    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 30)
    private DeliveryStatus previousStatus;

    @Column(name = "previous_attempt_count", nullable = false)
    private int previousAttemptCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_failure_type", length = 30)
    private FailureType previousFailureType;

    @Column(name = "previous_failure_code", length = 100)
    private String previousFailureCode;

    @Column(name = "previous_failure_message", length = 1000)
    private String previousFailureMessage;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected NotificationManualRetry() {
    }

    private NotificationManualRetry(
            NotificationDelivery notificationDelivery,
            int retryCycle,
            String requestedBy,
            String reason,
            DeliveryStatus previousStatus,
            int previousAttemptCount,
            FailureType previousFailureType,
            String previousFailureCode,
            String previousFailureMessage,
            LocalDateTime requestedAt
    ) {
        this.notificationDelivery = notificationDelivery;
        this.retryCycle = retryCycle;
        this.requestedBy = requestedBy;
        this.reason = reason;
        this.previousStatus = previousStatus;
        this.previousAttemptCount = previousAttemptCount;
        this.previousFailureType = previousFailureType;
        this.previousFailureCode = previousFailureCode;
        this.previousFailureMessage = previousFailureMessage;
        this.requestedAt = requestedAt;
    }

    public static NotificationManualRetry record(
            NotificationDelivery notificationDelivery,
            int retryCycle,
            String requestedBy,
            String reason,
            LocalDateTime requestedAt
    ) {
        return new NotificationManualRetry(
                notificationDelivery,
                retryCycle,
                requestedBy,
                reason,
                notificationDelivery.getStatus(),
                notificationDelivery.getAttemptCount(),
                notificationDelivery.getLastFailureType(),
                notificationDelivery.getLastFailureCode(),
                notificationDelivery.getLastFailureMessage(),
                requestedAt
        );
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public NotificationDelivery getNotificationDelivery() {
        return notificationDelivery;
    }

    public int getRetryCycle() {
        return retryCycle;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getReason() {
        return reason;
    }

    public DeliveryStatus getPreviousStatus() {
        return previousStatus;
    }

    public int getPreviousAttemptCount() {
        return previousAttemptCount;
    }

    public FailureType getPreviousFailureType() {
        return previousFailureType;
    }

    public String getPreviousFailureCode() {
        return previousFailureCode;
    }

    public String getPreviousFailureMessage() {
        return previousFailureMessage;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
