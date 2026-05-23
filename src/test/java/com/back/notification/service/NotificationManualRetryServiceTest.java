package com.back.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.notification.domain.NotificationAttempt;
import com.back.notification.domain.NotificationDelivery;
import com.back.notification.domain.NotificationManualRetry;
import com.back.notification.domain.NotificationRequest;
import com.back.notification.enums.AttemptResult;
import com.back.notification.enums.DeliveryChannel;
import com.back.notification.enums.DeliveryStatus;
import com.back.notification.enums.DispatchChannel;
import com.back.notification.enums.FailureType;
import com.back.notification.enums.NotificationType;
import com.back.notification.infrastructure.persistence.NotificationAttemptRepository;
import com.back.notification.infrastructure.persistence.NotificationDeliveryRepository;
import com.back.notification.infrastructure.persistence.NotificationInboxRepository;
import com.back.notification.infrastructure.persistence.NotificationManualRetryRepository;
import com.back.notification.infrastructure.persistence.NotificationRequestRepository;
import com.back.notification.web.dto.ManualRetryRequest;
import com.back.notification.web.dto.ManualRetryResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("알림 수동 재시도")
class NotificationManualRetryServiceTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 23, 12, 0);

    @Autowired
    private NotificationManualRetryService notificationManualRetryService;

    @Autowired
    private NotificationWorkerService notificationWorkerService;

    @Autowired
    private NotificationRequestRepository notificationRequestRepository;

    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Autowired
    private NotificationAttemptRepository notificationAttemptRepository;

    @Autowired
    private NotificationManualRetryRepository notificationManualRetryRepository;

    @Autowired
    private NotificationInboxRepository notificationInboxRepository;

    @BeforeEach
    void setUp() {
        deleteAll();
    }

    @AfterEach
    void tearDown() {
        deleteAll();
    }

    @Test
    @DisplayName("최종 실패 알림을 수동 재시도하면 attemptCount는 0으로 초기화되고 retryCycle은 증가한다")
    void manualRetryResetsCurrentAttemptCountAndIncreasesRetryCycle() {
        NotificationRequest request = saveRequest("user-manual-retry-1", "event-manual-retry-1");
        NotificationDelivery delivery = saveFailedDeliveryWithAttempt(request);

        ManualRetryResponse response = notificationManualRetryService.retryFailedNotification(
                request.getId(),
                "admin-1",
                new ManualRetryRequest("외부 서버 장애 복구 후 재시도")
        );

        NotificationDelivery retriedDelivery = notificationDeliveryRepository.findById(delivery.getId())
                .orElseThrow();
        List<NotificationManualRetry> manualRetries = notificationManualRetryRepository
                .findByNotificationDeliveryIdOrderByRetryCycleAsc(delivery.getId());

        assertThat(response.retriedDeliveryCount()).isEqualTo(1);
        assertThat(response.deliveries()).singleElement().satisfies(retried -> {
            assertThat(retried.deliveryId()).isEqualTo(delivery.getId());
            assertThat(retried.status()).isEqualTo(DeliveryStatus.PENDING);
            assertThat(retried.retryCycle()).isEqualTo(1);
            assertThat(retried.attemptCount()).isZero();
            assertThat(retried.manualRetryCount()).isEqualTo(1);
        });
        assertThat(retriedDelivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(retriedDelivery.getAttemptCount()).isZero();
        assertThat(retriedDelivery.getRetryCycle()).isEqualTo(1);
        assertThat(retriedDelivery.getManualRetryCount()).isEqualTo(1);
        assertThat(retriedDelivery.getFailedAt()).isNull();
        assertThat(manualRetries).singleElement().satisfies(manualRetry -> {
            assertThat(manualRetry.getRetryCycle()).isEqualTo(1);
            assertThat(manualRetry.getRequestedBy()).isEqualTo("admin-1");
            assertThat(manualRetry.getReason()).isEqualTo("외부 서버 장애 복구 후 재시도");
            assertThat(manualRetry.getPreviousStatus()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(manualRetry.getPreviousAttemptCount()).isEqualTo(1);
            assertThat(manualRetry.getPreviousFailureCode()).isEqualTo("SMTP_BUSY");
        });
    }

    @Test
    @DisplayName("수동 재시도 후 Worker가 새 retryCycle의 attemptNo 1을 기록해도 과거 이력과 충돌하지 않는다")
    void workerRecordsAttemptNoOneInNewRetryCycleWithoutConflictingWithOldAttempt() {
        NotificationRequest request = saveRequest("user-manual-retry-2", "event-manual-retry-2");
        NotificationDelivery delivery = saveFailedDeliveryWithAttempt(request);

        notificationManualRetryService.retryFailedNotification(
                request.getId(),
                "admin-2",
                new ManualRetryRequest("수동 재시도 후 성공 확인")
        );

        int processedCount = notificationWorkerService.processDueDeliveries();

        NotificationDelivery succeededDelivery = notificationDeliveryRepository.findById(delivery.getId())
                .orElseThrow();
        List<NotificationAttempt> attempts = notificationAttemptRepository
                .findByNotificationDeliveryIdOrderByAttemptNoAsc(delivery.getId());

        assertThat(processedCount).isEqualTo(1);
        assertThat(succeededDelivery.getStatus()).isEqualTo(DeliveryStatus.SUCCEEDED);
        assertThat(succeededDelivery.getRetryCycle()).isEqualTo(1);
        assertThat(succeededDelivery.getAttemptCount()).isEqualTo(1);
        assertThat(attempts).hasSize(2);
        assertThat(attempts)
                .extracting(NotificationAttempt::getRetryCycle, NotificationAttempt::getAttemptNo)
                .containsExactlyInAnyOrder(
                        tuple(0, 1),
                        tuple(1, 1)
                );
        assertThat(attempts)
                .filteredOn(attempt -> attempt.getRetryCycle() == 1)
                .singleElement()
                .satisfies(attempt -> assertThat(attempt.getResult()).isEqualTo(AttemptResult.SUCCEEDED));
    }

    @Test
    @DisplayName("최종 실패 상태가 아니면 수동 재시도를 허용하지 않는다")
    void manualRetryRequiresFailedDelivery() {
        NotificationRequest request = saveRequest("user-manual-retry-3", "event-manual-retry-3");
        notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.createPending(
                        request,
                        DeliveryChannel.EMAIL,
                        5,
                        BASE_TIME
                )
        );

        assertThatThrownBy(() -> notificationManualRetryService.retryFailedNotification(
                request.getId(),
                "admin-3",
                new ManualRetryRequest("아직 실패하지 않은 알림 재시도")
        ))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_MANUAL_RETRY_NOT_ALLOWED);
    }

    private NotificationRequest saveRequest(String recipientId, String eventId) {
        return notificationRequestRepository.saveAndFlush(
                NotificationRequest.create(
                        recipientId,
                        NotificationType.PAYMENT_CONFIRMED,
                        eventId,
                        DispatchChannel.EMAIL,
                        "{}"
                )
        );
    }

    private NotificationDelivery saveFailedDeliveryWithAttempt(NotificationRequest request) {
        NotificationDelivery delivery = NotificationDelivery.createPending(
                request,
                DeliveryChannel.EMAIL,
                1,
                BASE_TIME
        );
        delivery.claim("failed-worker", BASE_TIME.plusSeconds(30));
        delivery.markFailed(
                FailureType.RETRYABLE,
                "SMTP_BUSY",
                "SMTP 서버가 계속 바쁩니다",
                BASE_TIME.plusSeconds(5)
        );
        NotificationDelivery savedDelivery = notificationDeliveryRepository.saveAndFlush(delivery);

        NotificationAttempt attempt = NotificationAttempt.start(
                savedDelivery,
                savedDelivery.getRetryCycle(),
                savedDelivery.getAttemptCount(),
                "failed-worker",
                BASE_TIME
        );
        attempt.markFailed(
                AttemptResult.RETRYABLE_FAILED,
                FailureType.RETRYABLE,
                "SMTP_BUSY",
                "SMTP 서버가 계속 바쁩니다",
                BASE_TIME.plusSeconds(5)
        );
        notificationAttemptRepository.saveAndFlush(attempt);
        return savedDelivery;
    }

    private void deleteAll() {
        notificationInboxRepository.deleteAll();
        notificationManualRetryRepository.deleteAll();
        notificationAttemptRepository.deleteAll();
        notificationDeliveryRepository.deleteAll();
        notificationRequestRepository.deleteAll();
    }
}
