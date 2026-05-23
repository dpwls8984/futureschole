package com.back.notification.service;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.notification.infrastructure.persistence.NotificationInboxRepository;
import com.back.notification.web.dto.MarkNotificationsReadRequest;
import com.back.notification.web.dto.MarkReadResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationInboxCommandService {

    private final NotificationInboxRepository notificationInboxRepository;
    private final Clock clock;

    public NotificationInboxCommandService(
            NotificationInboxRepository notificationInboxRepository,
            Clock clock
    ) {
        this.notificationInboxRepository = notificationInboxRepository;
        this.clock = clock;
    }

    @Transactional
    public MarkReadResponse markVisibleNotificationsRead(
            String currentUserId,
            MarkNotificationsReadRequest request
    ) {
        if (currentUserId == null || currentUserId.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Set<Long> inboxIds = new LinkedHashSet<>(request.inboxIds());
        LocalDateTime readAt = LocalDateTime.now(clock);
        int markedCount = notificationInboxRepository.markVisibleNotificationsRead(
                currentUserId,
                inboxIds,
                readAt
        );

        return new MarkReadResponse(
                currentUserId,
                inboxIds.size(),
                markedCount,
                readAt
        );
    }
}
