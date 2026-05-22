package com.back.notification.service;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.notification.config.NotificationProperties;
import com.back.notification.domain.NotificationAttempt;
import com.back.notification.domain.NotificationDelivery;
import com.back.notification.domain.RetryPolicy;
import com.back.notification.enums.AttemptResult;
import com.back.notification.enums.DeliveryChannel;
import com.back.notification.enums.DeliveryStatus;
import com.back.notification.enums.FailureType;
import com.back.notification.infrastructure.persistence.NotificationAttemptRepository;
import com.back.notification.infrastructure.persistence.NotificationClaimRepository;
import com.back.notification.infrastructure.persistence.NotificationDeliveryRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class NotificationWorkerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationWorkerService.class);
    private static final List<DeliveryStatus> CLAIMABLE_STATUSES = List.of(
            DeliveryStatus.PENDING,
            DeliveryStatus.RETRY_WAITING
    );

    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationAttemptRepository notificationAttemptRepository;
    private final NotificationClaimRepository notificationClaimRepository;
    private final MessageRenderer messageRenderer;
    private final RetryPolicy retryPolicy;
    private final NotificationProperties notificationProperties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final Map<DeliveryChannel, ChannelDispatcher> dispatchers;

    public NotificationWorkerService(
            NotificationDeliveryRepository notificationDeliveryRepository,
            NotificationAttemptRepository notificationAttemptRepository,
            NotificationClaimRepository notificationClaimRepository,
            MessageRenderer messageRenderer,
            RetryPolicy retryPolicy,
            NotificationProperties notificationProperties,
            TransactionTemplate transactionTemplate,
            Clock clock,
            List<ChannelDispatcher> channelDispatchers
    ) {
        this.notificationDeliveryRepository = notificationDeliveryRepository;
        this.notificationAttemptRepository = notificationAttemptRepository;
        this.notificationClaimRepository = notificationClaimRepository;
        this.messageRenderer = messageRenderer;
        this.retryPolicy = retryPolicy;
        this.notificationProperties = notificationProperties;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.dispatchers = toDispatcherMap(channelDispatchers);
    }

    public int processDueDeliveries() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> deliveryIds = notificationDeliveryRepository.findDueDeliveryIds(
                CLAIMABLE_STATUSES,
                now,
                PageRequest.of(0, notificationProperties.getWorker().getBatchSize())
        );

        int processedCount = 0;
        for (Long deliveryId : deliveryIds) {
            if (processDelivery(deliveryId)) {
                processedCount++;
            }
        }
        return processedCount;
    }

    public boolean processDelivery(Long deliveryId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<ClaimedDelivery> claimedDelivery = transactionTemplate.execute(
                status -> claimDelivery(deliveryId, now)
        );

        if (claimedDelivery == null || claimedDelivery.isEmpty()) {
            return false;
        }

        transactionTemplate.executeWithoutResult(status -> dispatchAndRecordResult(claimedDelivery.get()));
        return true;
    }

    private Optional<ClaimedDelivery> claimDelivery(Long deliveryId, LocalDateTime now) {
        int affectedRows = notificationClaimRepository.claimDelivery(
                deliveryId,
                workerId(),
                now.plus(notificationProperties.getWorker().getLockDuration()),
                now
        );
        if (affectedRows == 0) {
            return Optional.empty();
        }

        NotificationDelivery delivery = notificationDeliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ServiceException(ErrorCode.DELIVERY_NOT_FOUND));
        NotificationAttempt attempt = NotificationAttempt.start(
                delivery,
                delivery.getAttemptCount(),
                workerId(),
                now
        );
        NotificationAttempt savedAttempt = notificationAttemptRepository.save(attempt);

        return Optional.of(new ClaimedDelivery(delivery.getId(), savedAttempt.getId()));
    }

    private void dispatchAndRecordResult(ClaimedDelivery claimedDelivery) {
        NotificationDelivery delivery = notificationDeliveryRepository
                .findByIdWithRequest(claimedDelivery.deliveryId())
                .orElseThrow(() -> new ServiceException(ErrorCode.DELIVERY_NOT_FOUND));
        NotificationAttempt attempt = notificationAttemptRepository.findById(claimedDelivery.attemptId())
                .orElseThrow(() -> new ServiceException(ErrorCode.DELIVERY_NOT_FOUND));

        RenderedMessage message = messageRenderer.render(delivery.getNotificationRequest(), delivery.getChannel());
        DispatchResult result = dispatch(delivery, message);
        LocalDateTime finishedAt = LocalDateTime.now(clock);

        if (result instanceof DispatchResult.Success) {
            delivery.markSucceeded(finishedAt);
            attempt.markSucceeded(finishedAt);
            return;
        }

        if (result instanceof DispatchResult.PermanentFailure failure) {
            markPermanentFailure(delivery, attempt, failure, finishedAt);
            return;
        }

        if (result instanceof DispatchResult.RetryableFailure failure) {
            markRetryableFailure(delivery, attempt, failure, finishedAt);
        }
    }

    private DispatchResult dispatch(NotificationDelivery delivery, RenderedMessage message) {
        ChannelDispatcher dispatcher = dispatchers.get(delivery.getChannel());
        if (dispatcher == null) {
            return DispatchResult.permanentFailure(
                    "DISPATCHER_NOT_FOUND",
                    "지원하지 않는 발송 채널입니다: " + delivery.getChannel()
            );
        }

        try {
            return dispatcher.dispatch(delivery, message);
        } catch (RuntimeException e) {
            log.warn(
                    "알림 발송 중 예외가 발생해 재시도 대상으로 기록합니다. deliveryId={}, channel={}, error={}",
                    delivery.getId(),
                    delivery.getChannel(),
                    e.toString()
            );
            return DispatchResult.retryableFailure("DISPATCH_EXCEPTION", truncate(e.getMessage()));
        }
    }

    private void markPermanentFailure(
            NotificationDelivery delivery,
            NotificationAttempt attempt,
            DispatchResult.PermanentFailure failure,
            LocalDateTime finishedAt
    ) {
        delivery.markFailed(FailureType.PERMANENT, failure.code(), failure.message(), finishedAt);
        attempt.markFailed(
                AttemptResult.PERMANENT_FAILED,
                FailureType.PERMANENT,
                failure.code(),
                failure.message(),
                finishedAt
        );
    }

    private void markRetryableFailure(
            NotificationDelivery delivery,
            NotificationAttempt attempt,
            DispatchResult.RetryableFailure failure,
            LocalDateTime finishedAt
    ) {
        attempt.markFailed(
                AttemptResult.RETRYABLE_FAILED,
                FailureType.RETRYABLE,
                failure.code(),
                failure.message(),
                finishedAt
        );

        if (delivery.canRetry()) {
            delivery.markRetryWaiting(
                    FailureType.RETRYABLE,
                    failure.code(),
                    failure.message(),
                    retryPolicy.nextAvailableAt(delivery.getAttemptCount(), finishedAt)
            );
            return;
        }

        delivery.markFailed(FailureType.RETRYABLE, failure.code(), failure.message(), finishedAt);
    }

    private Map<DeliveryChannel, ChannelDispatcher> toDispatcherMap(List<ChannelDispatcher> channelDispatchers) {
        Map<DeliveryChannel, ChannelDispatcher> mappedDispatchers = new EnumMap<>(DeliveryChannel.class);
        for (ChannelDispatcher dispatcher : channelDispatchers) {
            ChannelDispatcher previous = mappedDispatchers.put(dispatcher.channel(), dispatcher);
            if (previous != null) {
                throw new IllegalStateException("Duplicate dispatcher channel: " + dispatcher.channel());
            }
        }
        return mappedDispatchers;
    }

    private String workerId() {
        return notificationProperties.getWorker().getId();
    }

    private String truncate(String message) {
        if (message == null) {
            return "알림 발송 중 알 수 없는 예외가 발생했습니다.";
        }
        if (message.length() <= 1000) {
            return message;
        }
        return message.substring(0, 1000);
    }

    private record ClaimedDelivery(
            Long deliveryId,
            Long attemptId
    ) {
    }
}
