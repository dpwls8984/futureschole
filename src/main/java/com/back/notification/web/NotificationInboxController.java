package com.back.notification.web;

import com.back.notification.enums.InboxReadStatus;
import com.back.notification.service.NotificationInboxCommandService;
import com.back.notification.service.NotificationInboxQueryService;
import com.back.notification.web.dto.InboxNotificationListResponse;
import com.back.notification.web.dto.MarkNotificationsReadRequest;
import com.back.notification.web.dto.MarkReadResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/notifications")
public class NotificationInboxController implements NotificationInboxControllerSpec {

    private final NotificationInboxQueryService notificationInboxQueryService;
    private final NotificationInboxCommandService notificationInboxCommandService;

    public NotificationInboxController(
            NotificationInboxQueryService notificationInboxQueryService,
            NotificationInboxCommandService notificationInboxCommandService
    ) {
        this.notificationInboxQueryService = notificationInboxQueryService;
        this.notificationInboxCommandService = notificationInboxCommandService;
    }

    @Override
    @GetMapping
    public ResponseEntity<InboxNotificationListResponse> getMyNotifications(
            @RequestHeader("X-User-Id") String currentUserId,
            @RequestParam(defaultValue = "ALL") InboxReadStatus readStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(notificationInboxQueryService.getMyNotifications(
                currentUserId,
                readStatus,
                page,
                size
        ));
    }

    @Override
    @PatchMapping("/read")
    public ResponseEntity<MarkReadResponse> markVisibleNotificationsRead(
            @RequestHeader("X-User-Id") String currentUserId,
            @Valid @RequestBody MarkNotificationsReadRequest request
    ) {
        return ResponseEntity.ok(notificationInboxCommandService.markVisibleNotificationsRead(
                currentUserId,
                request
        ));
    }
}
