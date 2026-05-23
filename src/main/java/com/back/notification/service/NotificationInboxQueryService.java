package com.back.notification.service;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.notification.domain.NotificationDelivery;
import com.back.notification.domain.NotificationInbox;
import com.back.notification.domain.NotificationRequest;
import com.back.notification.enums.InboxReadStatus;
import com.back.notification.infrastructure.persistence.NotificationInboxRepository;
import com.back.notification.web.dto.InboxNotificationListResponse;
import com.back.notification.web.dto.InboxNotificationResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationInboxQueryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationInboxRepository notificationInboxRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NotificationInboxQueryService(
            NotificationInboxRepository notificationInboxRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.notificationInboxRepository = notificationInboxRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public InboxNotificationListResponse getMyNotifications(
            String currentUserId,
            InboxReadStatus readStatus,
            int page,
            int size
    ) {
        validate(currentUserId, page, size);

        InboxReadStatus filter = readStatus == null ? InboxReadStatus.ALL : readStatus;
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("visibleAt"), Sort.Order.desc("id"))
        );
        LocalDateTime now = LocalDateTime.now(clock);
        Page<NotificationInbox> inboxPage = findInboxPage(currentUserId, filter, now, pageRequest);

        return new InboxNotificationListResponse(
                currentUserId,
                filter,
                inboxPage.getNumber(),
                inboxPage.getSize(),
                inboxPage.getTotalElements(),
                inboxPage.getTotalPages(),
                inboxPage.hasNext(),
                inboxPage.getContent().stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    private Page<NotificationInbox> findInboxPage(
            String currentUserId,
            InboxReadStatus readStatus,
            LocalDateTime now,
            PageRequest pageRequest
    ) {
        return switch (readStatus) {
            case ALL -> notificationInboxRepository.findVisibleByRecipientId(currentUserId, now, pageRequest);
            case READ -> notificationInboxRepository.findVisibleReadByRecipientId(currentUserId, now, pageRequest);
            case UNREAD -> notificationInboxRepository.findVisibleUnreadByRecipientId(currentUserId, now, pageRequest);
        };
    }

    private InboxNotificationResponse toResponse(NotificationInbox inbox) {
        NotificationDelivery delivery = inbox.getNotificationDelivery();
        NotificationRequest request = delivery.getNotificationRequest();

        return new InboxNotificationResponse(
                inbox.getId(),
                request.getId(),
                delivery.getId(),
                request.getNotificationType(),
                request.getEventId(),
                inbox.getTitle(),
                inbox.getMessage(),
                toReferenceData(request.getReferenceDataJson()),
                inbox.isRead(),
                inbox.getVisibleAt(),
                inbox.getReadAt(),
                inbox.getCreatedAt()
        );
    }

    private void validate(String currentUserId, int page, int size) {
        if (currentUserId == null || currentUserId.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private Map<String, Object> toReferenceData(String referenceDataJson) {
        try {
            return objectMapper.readValue(referenceDataJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
