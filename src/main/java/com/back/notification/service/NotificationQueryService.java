package com.back.notification.service;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.notification.domain.NotificationDelivery;
import com.back.notification.domain.NotificationRequest;
import com.back.notification.infrastructure.persistence.NotificationDeliveryRepository;
import com.back.notification.infrastructure.persistence.NotificationRequestRepository;
import com.back.notification.web.dto.NotificationDetailResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationQueryService {

    private final NotificationRequestRepository notificationRequestRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final ObjectMapper objectMapper;
    private final NotificationResponseMapper responseMapper;

    public NotificationQueryService(
            NotificationRequestRepository notificationRequestRepository,
            NotificationDeliveryRepository notificationDeliveryRepository,
            ObjectMapper objectMapper,
            NotificationResponseMapper responseMapper
    ) {
        this.notificationRequestRepository = notificationRequestRepository;
        this.notificationDeliveryRepository = notificationDeliveryRepository;
        this.objectMapper = objectMapper;
        this.responseMapper = responseMapper;
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse getNotificationDetail(Long notificationId) {
        NotificationRequest request = notificationRequestRepository.findById(notificationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.NOTIFICATION_NOT_FOUND));
        List<NotificationDelivery> deliveries = notificationDeliveryRepository.findByNotificationRequestId(request.getId());

        return new NotificationDetailResponse(
                request.getId(),
                request.getRecipientId(),
                request.getNotificationType(),
                request.getEventId(),
                request.getRequestedChannel(),
                toReferenceData(request.getReferenceDataJson()),
                responseMapper.aggregateStatus(deliveries),
                responseMapper.toDeliveryResponses(deliveries),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
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
