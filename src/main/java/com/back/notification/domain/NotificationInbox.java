package com.back.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_inboxes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_inbox_delivery",
                        columnNames = "notification_delivery_id"
                )
        }
)
public class NotificationInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_delivery_id", nullable = false)
    private NotificationDelivery notificationDelivery;

    @Column(name = "recipient_id", nullable = false, length = 100)
    private String recipientId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "visible_at", nullable = false)
    private LocalDateTime visibleAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected NotificationInbox() {
    }

    private NotificationInbox(
            NotificationDelivery notificationDelivery,
            String recipientId,
            String title,
            String message,
            LocalDateTime visibleAt
    ) {
        this.notificationDelivery = notificationDelivery;
        this.recipientId = recipientId;
        this.title = title;
        this.message = message;
        this.visibleAt = visibleAt;
    }

    public static NotificationInbox create(
            NotificationDelivery notificationDelivery,
            String recipientId,
            String title,
            String message,
            LocalDateTime visibleAt
    ) {
        return new NotificationInbox(notificationDelivery, recipientId, title, message, visibleAt);
    }

    public void markRead(LocalDateTime readAt) {
        if (this.readAt == null) {
            this.readAt = readAt;
        }
    }

    public boolean isRead() {
        return readAt != null;
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

    public NotificationDelivery getNotificationDelivery() {
        return notificationDelivery;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getVisibleAt() {
        return visibleAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
