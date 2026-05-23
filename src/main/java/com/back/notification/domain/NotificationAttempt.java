package com.back.notification.domain;

import com.back.notification.enums.AttemptResult;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_attempts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_attempt_no",
                        columnNames = {"notification_delivery_id", "retry_cycle", "attempt_no"}
                )
        }
)
public class NotificationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_delivery_id", nullable = false)
    private NotificationDelivery notificationDelivery;

    @Column(name = "retry_cycle", nullable = false)
    private int retryCycle;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 30)
    private AttemptResult result;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_type", length = 30)
    private FailureType failureType;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 1000)
    private String failureMessage;

    @Column(name = "worker_id", length = 100)
    private String workerId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    protected NotificationAttempt() {
    }

    private NotificationAttempt(
            NotificationDelivery notificationDelivery,
            int retryCycle,
            int attemptNo,
            String workerId,
            LocalDateTime startedAt
    ) {
        this.notificationDelivery = notificationDelivery;
        this.retryCycle = retryCycle;
        this.attemptNo = attemptNo;
        this.workerId = workerId;
        this.startedAt = startedAt;
    }

    public static NotificationAttempt start(
            NotificationDelivery notificationDelivery,
            int retryCycle,
            int attemptNo,
            String workerId,
            LocalDateTime startedAt
    ) {
        return new NotificationAttempt(notificationDelivery, retryCycle, attemptNo, workerId, startedAt);
    }

    public void markSucceeded(LocalDateTime finishedAt) {
        this.result = AttemptResult.SUCCEEDED;
        this.finishedAt = finishedAt;
        clearFailure();
    }

    public void markFailed(
            AttemptResult result,
            FailureType failureType,
            String failureCode,
            String failureMessage,
            LocalDateTime finishedAt
    ) {
        this.result = result;
        this.failureType = failureType;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.finishedAt = finishedAt;
    }

    private void clearFailure() {
        this.failureType = null;
        this.failureCode = null;
        this.failureMessage = null;
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

    public int getAttemptNo() {
        return attemptNo;
    }

    public AttemptResult getResult() {
        return result;
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public String getWorkerId() {
        return workerId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }
}
