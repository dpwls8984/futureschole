package com.back.notification.web;

import com.back.notification.service.NotificationCommandService;
import com.back.notification.service.NotificationManualRetryService;
import com.back.notification.service.NotificationQueryService;
import com.back.notification.web.dto.CreateNotificationRequest;
import com.back.notification.web.dto.CreateNotificationResponse;
import com.back.notification.web.dto.ManualRetryRequest;
import com.back.notification.web.dto.ManualRetryResponse;
import com.back.notification.web.dto.NotificationDetailResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController implements NotificationControllerSpec {

    private final NotificationCommandService notificationCommandService;
    private final NotificationQueryService notificationQueryService;
    private final NotificationManualRetryService notificationManualRetryService;

    public NotificationController(
            NotificationCommandService notificationCommandService,
            NotificationQueryService notificationQueryService,
            NotificationManualRetryService notificationManualRetryService
    ) {
        this.notificationCommandService = notificationCommandService;
        this.notificationQueryService = notificationQueryService;
        this.notificationManualRetryService = notificationManualRetryService;
    }

    @Override
    @PostMapping
    public ResponseEntity<CreateNotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request
    ) {
        CreateNotificationResponse response = notificationCommandService.createNotification(request);
        HttpStatus status = response.duplicated() ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(response);
    }

    @Override
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationDetailResponse> getNotificationDetail(
            @PathVariable Long notificationId
    ) {
        return ResponseEntity.ok(notificationQueryService.getNotificationDetail(notificationId));
    }

    @Override
    @PostMapping("/{notificationId}/retry")
    public ResponseEntity<ManualRetryResponse> retryFailedNotification(
            @PathVariable Long notificationId,
            @RequestHeader("X-Admin-Id") String requestedBy,
            @Valid @RequestBody ManualRetryRequest request
    ) {
        return ResponseEntity.ok(notificationManualRetryService.retryFailedNotification(
                notificationId,
                requestedBy,
                request
        ));
    }
}
