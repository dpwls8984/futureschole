package com.back.notification.service;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.notification.config.NotificationProperties;
import com.back.notification.domain.NotificationDelivery;
import com.back.notification.domain.NotificationRequest;
import com.back.notification.infrastructure.persistence.NotificationDeliveryRepository;
import com.back.notification.infrastructure.persistence.NotificationRequestRepository;
import com.back.notification.web.dto.CreateNotificationRequest;
import com.back.notification.web.dto.CreateNotificationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class NotificationCommandService {

    private final NotificationRequestRepository notificationRequestRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationProperties notificationProperties;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final NotificationResponseMapper responseMapper;

    public NotificationCommandService(
            NotificationRequestRepository notificationRequestRepository,
            NotificationDeliveryRepository notificationDeliveryRepository,
            NotificationProperties notificationProperties,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            Clock clock,
            NotificationResponseMapper responseMapper
    ) {
        this.notificationRequestRepository = notificationRequestRepository;
        this.notificationDeliveryRepository = notificationDeliveryRepository;
        this.notificationProperties = notificationProperties;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.responseMapper = responseMapper;
    }

    public CreateNotificationResponse createNotification(CreateNotificationRequest request) {
        CreateNotificationResponse existingResponse = findExistingNotificationResponse(request);
        if (existingResponse != null) {
            return existingResponse;
        }

        try {
            return transactionTemplate.execute(status -> createNewNotification(request));
        } catch (DataIntegrityViolationException e) {
            return transactionTemplate.execute(status -> handleDuplicatedNotification(request));
        }
    }

    private CreateNotificationResponse findExistingNotificationResponse(CreateNotificationRequest request) {
        return transactionTemplate.execute(status -> notificationRequestRepository
                .findByRecipientIdAndNotificationTypeAndEventId(
                        request.recipientId(),
                        request.notificationType(),
                        request.eventId()
                )
                .map(existingRequest -> toDuplicatedResponseOrThrow(request, existingRequest))
                .orElse(null));
    }

    private CreateNotificationResponse createNewNotification(CreateNotificationRequest request) {
        NotificationRequest notificationRequest = NotificationRequest.create(
                request.recipientId(),
                request.notificationType(),
                request.eventId(),
                request.channel(),
                toJson(request.referenceData())
        );
        NotificationRequest savedRequest = notificationRequestRepository.saveAndFlush(notificationRequest);

        LocalDateTime now = LocalDateTime.now(clock);
        List<NotificationDelivery> deliveries = request.channel().deliveryChannels().stream()
                .map(channel -> NotificationDelivery.createPending(
                        savedRequest,
                        channel,
                        notificationProperties.getRetry().getMaxAttempts(),
                        now
                ))
                .toList();
        List<NotificationDelivery> savedDeliveries = notificationDeliveryRepository.saveAllAndFlush(deliveries);

        return toCreateResponse(savedRequest, savedDeliveries, false);
    }

    private CreateNotificationResponse handleDuplicatedNotification(CreateNotificationRequest request) {
        NotificationRequest existingRequest = notificationRequestRepository
                .findByRecipientIdAndNotificationTypeAndEventId(
                        request.recipientId(),
                        request.notificationType(),
                        request.eventId()
                )
                .orElseThrow(() -> new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR));

        return toDuplicatedResponseOrThrow(request, existingRequest);
    }

    private CreateNotificationResponse toDuplicatedResponseOrThrow(
            CreateNotificationRequest request,
            NotificationRequest existingRequest
    ) {
        if (existingRequest.getRequestedChannel() != request.channel()) {
            throw new ServiceException(ErrorCode.NOTIFICATION_CHANNEL_CONFLICT);
        }

        List<NotificationDelivery> deliveries = notificationDeliveryRepository
                .findByNotificationRequestId(existingRequest.getId());
        return toCreateResponse(existingRequest, deliveries, true);
    }

    private String toJson(Map<String, Object> referenceData) {
        try {
            return objectMapper.writeValueAsString(referenceData == null ? Map.of() : referenceData);
        } catch (JsonProcessingException e) {
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private CreateNotificationResponse toCreateResponse(
            NotificationRequest request,
            List<NotificationDelivery> deliveries,
            boolean duplicated
    ) {
        return new CreateNotificationResponse(
                request.getId(),
                request.getRecipientId(),
                request.getNotificationType(),
                request.getEventId(),
                request.getRequestedChannel(),
                responseMapper.aggregateStatus(deliveries),
                responseMapper.toDeliveryResponses(deliveries),
                duplicated,
                request.getCreatedAt()
        );
    }
}
