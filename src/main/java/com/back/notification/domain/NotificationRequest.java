package com.back.notification.domain;

import com.back.notification.enums.DispatchChannel;
import com.back.notification.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_requests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_request_dedup",
                        columnNames = {"recipient_id", "notification_type", "event_id"}
                )
        }
)
public class NotificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false, length = 100)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 80)
    private NotificationType notificationType;

    @Column(name = "event_id", nullable = false, length = 150)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_channel", nullable = false, length = 40)
    private DispatchChannel requestedChannel;

    @Column(name = "reference_data_json", nullable = false, columnDefinition = "text")
    private String referenceDataJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected NotificationRequest() {
    }

    private NotificationRequest(
            String recipientId,
            NotificationType notificationType,
            String eventId,
            DispatchChannel requestedChannel,
            String referenceDataJson
    ) {
        this.recipientId = recipientId;
        this.notificationType = notificationType;
        this.eventId = eventId;
        this.requestedChannel = requestedChannel;
        this.referenceDataJson = referenceDataJson;
    }

    public static NotificationRequest create(
            String recipientId,
            NotificationType notificationType,
            String eventId,
            DispatchChannel requestedChannel,
            String referenceDataJson
    ) {
        return new NotificationRequest(
                recipientId,
                notificationType,
                eventId,
                requestedChannel,
                referenceDataJson
        );
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

    public String getRecipientId() {
        return recipientId;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public String getEventId() {
        return eventId;
    }

    public DispatchChannel getRequestedChannel() {
        return requestedChannel;
    }

    public String getReferenceDataJson() {
        return referenceDataJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
