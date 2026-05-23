package com.back.notification.domain;

import com.back.notification.enums.DeliveryChannel;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_deliveries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_delivery_channel",
                        columnNames = {"notification_request_id", "channel"}
                )
        }
)
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_request_id", nullable = false)
    private NotificationRequest notificationRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 40)
    private DeliveryChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "retry_cycle", nullable = false)
    private int retryCycle;

    @Column(name = "manual_retry_count", nullable = false)
    private int manualRetryCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    @Column(name = "lock_owner", length = 100)
    private String lockOwner;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_failure_type", length = 30)
    private FailureType lastFailureType;

    @Column(name = "last_failure_code", length = 100)
    private String lastFailureCode;

    @Column(name = "last_failure_message", length = 1000)
    private String lastFailureMessage;

    @Column(name = "succeeded_at")
    private LocalDateTime succeededAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected NotificationDelivery() {
    }

    private NotificationDelivery(
            NotificationRequest notificationRequest,
            DeliveryChannel channel,
            int maxAttempts,
            LocalDateTime availableAt
    ) {
        this.notificationRequest = notificationRequest;
        this.channel = channel;
        this.status = DeliveryStatus.PENDING;
        this.attemptCount = 0;
        this.retryCycle = 0;
        this.manualRetryCount = 0;
        this.maxAttempts = maxAttempts;
        this.availableAt = availableAt;
    }

    public static NotificationDelivery createPending(
            NotificationRequest notificationRequest,
            DeliveryChannel channel,
            int maxAttempts,
            LocalDateTime availableAt
    ) {
        return new NotificationDelivery(notificationRequest, channel, maxAttempts, availableAt);
    }

    public void claim(String lockOwner, LocalDateTime lockedUntil) {
        this.status = DeliveryStatus.PROCESSING;
        this.attemptCount++;
        this.lockOwner = lockOwner;
        this.lockedUntil = lockedUntil;
    }

    public void markSucceeded(LocalDateTime succeededAt) {
        this.status = DeliveryStatus.SUCCEEDED;
        this.succeededAt = succeededAt;
        clearLock();
        clearFailure();
    }

    public void markRetryWaiting(
            FailureType failureType,
            String failureCode,
            String failureMessage,
            LocalDateTime nextAvailableAt
    ) {
        this.status = DeliveryStatus.RETRY_WAITING;
        this.availableAt = nextAvailableAt;
        this.lastFailureType = failureType;
        this.lastFailureCode = failureCode;
        this.lastFailureMessage = failureMessage;
        clearLock();
    }

    public void markFailed(
            FailureType failureType,
            String failureCode,
            String failureMessage,
            LocalDateTime failedAt
    ) {
        this.status = DeliveryStatus.FAILED;
        this.failedAt = failedAt;
        this.lastFailureType = failureType;
        this.lastFailureCode = failureCode;
        this.lastFailureMessage = failureMessage;
        clearLock();
    }

    public void markManualRetryScheduled(LocalDateTime availableAt) {
        this.status = DeliveryStatus.PENDING;
        this.retryCycle++;
        this.manualRetryCount++;
        this.attemptCount = 0;
        this.availableAt = availableAt;
        this.failedAt = null;
        clearLock();
    }

    public boolean canRetry() {
        return attemptCount < maxAttempts;
    }

    private void clearLock() {
        this.lockOwner = null;
        this.lockedUntil = null;
    }

    private void clearFailure() {
        this.lastFailureType = null;
        this.lastFailureCode = null;
        this.lastFailureMessage = null;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public NotificationRequest getNotificationRequest() {
        return notificationRequest;
    }

    public DeliveryChannel getChannel() {
        return channel;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getRetryCycle() {
        return retryCycle;
    }

    public int getManualRetryCount() {
        return manualRetryCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public LocalDateTime getAvailableAt() {
        return availableAt;
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public FailureType getLastFailureType() {
        return lastFailureType;
    }

    public String getLastFailureCode() {
        return lastFailureCode;
    }

    public String getLastFailureMessage() {
        return lastFailureMessage;
    }

    public LocalDateTime getSucceededAt() {
        return succeededAt;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
