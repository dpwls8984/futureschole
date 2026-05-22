package com.back.notification.web.dto;

import com.back.notification.enums.DispatchChannel;
import com.back.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateNotificationRequest(
        @NotBlank String recipientId,
        @NotNull NotificationType notificationType,
        @NotBlank String eventId,
        @NotNull DispatchChannel channel,
        Map<String, Object> referenceData
) {
}
