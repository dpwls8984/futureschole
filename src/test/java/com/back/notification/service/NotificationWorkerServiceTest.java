package com.back.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.global.config.ClockConfig;
import com.back.notification.domain.NotificationDelivery;
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
import com.back.notification.infrastructure.persistence.NotificationRequestRepository;
import com.back.notification.web.dto.CreateNotificationRequest;
import com.back.notification.web.dto.CreateNotificationResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
@Import(NotificationWorkerServiceTest.WorkerTestConfiguration.class)
@DisplayName("알림 발송 Worker Service")
class NotificationWorkerServiceTest {

    private static final LocalDateTime WORKER_NOW = LocalDateTime.of(2026, 5, 22, 10, 0);

    @Autowired
    private NotificationWorkerService notificationWorkerService;

    @Autowired
    private TestEmailDispatcher emailDispatcher;

    @Autowired
    private NotificationCommandService notificationCommandService;

    @Autowired
    private NotificationRequestRepository notificationRequestRepository;

    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Autowired
    private NotificationAttemptRepository notificationAttemptRepository;

    @Autowired
    private NotificationInboxRepository notificationInboxRepository;

    @BeforeEach
    void setUp() {
        notificationInboxRepository.deleteAll();
        notificationAttemptRepository.deleteAll();
        notificationDeliveryRepository.deleteAll();
        notificationRequestRepository.deleteAll();
        emailDispatcher.reset();
    }

    @Test
    @DisplayName("대기 중인 이메일 발송 작업을 처리하면 성공 상태와 시도 이력이 기록된다")
    void processDueEmailDeliveryRecordsSuccessAndAttemptHistory() {
        CreateNotificationResponse response = notificationCommandService.createNotification(
                new CreateNotificationRequest(
                        "user-worker-email-1",
                        NotificationType.PAYMENT_CONFIRMED,
                        "payment-worker-email-1",
                        DispatchChannel.EMAIL,
                        Map.of("paymentId", "payment-worker-email-1")
                )
        );

        int processedCount = notificationWorkerService.processDueDeliveries();

        List<NotificationDelivery> deliveries = notificationDeliveryRepository
                .findByNotificationRequestId(response.notificationId());
        assertThat(processedCount).isEqualTo(1);
        assertThat(deliveries).singleElement().satisfies(delivery -> {
            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SUCCEEDED);
            assertThat(delivery.getAttemptCount()).isEqualTo(1);
            assertThat(delivery.getLockOwner()).isNull();
            assertThat(delivery.getLockedUntil()).isNull();
            assertThat(delivery.getSucceededAt()).isNotNull();
        });

        Long deliveryId = deliveries.get(0).getId();
        assertThat(notificationAttemptRepository.findByNotificationDeliveryIdOrderByAttemptNoAsc(deliveryId))
                .singleElement()
                .satisfies(attempt -> {
                    assertThat(attempt.getAttemptNo()).isEqualTo(1);
                    assertThat(attempt.getResult()).isEqualTo(AttemptResult.SUCCEEDED);
                    assertThat(attempt.getWorkerId()).isEqualTo("test-worker");
                    assertThat(attempt.getFinishedAt()).isNotNull();
                });
        assertThat(notificationInboxRepository.count()).isZero();
    }

    @Test
    @DisplayName("대기 중인 인앱 발송 작업을 처리하면 사용자 알림함에 노출된다")
    void processDueInAppDeliveryCreatesInboxNotification() {
        CreateNotificationResponse response = notificationCommandService.createNotification(
                new CreateNotificationRequest(
                        "user-worker-in-app-1",
                        NotificationType.COURSE_STARTING_TOMORROW,
                        "course-worker-in-app-1",
                        DispatchChannel.IN_APP,
                        Map.of("courseId", "course-worker-in-app-1")
                )
        );

        int processedCount = notificationWorkerService.processDueDeliveries();

        NotificationDelivery delivery = notificationDeliveryRepository
                .findByNotificationRequestId(response.notificationId())
                .get(0);
        assertThat(processedCount).isEqualTo(1);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SUCCEEDED);
        assertThat(notificationAttemptRepository.findByNotificationDeliveryIdOrderByAttemptNoAsc(delivery.getId()))
                .singleElement()
                .satisfies(attempt -> assertThat(attempt.getResult()).isEqualTo(AttemptResult.SUCCEEDED));
        assertThat(notificationInboxRepository.findByNotificationDeliveryId(delivery.getId()))
                .hasValueSatisfying(inbox -> {
                    assertThat(inbox.getRecipientId()).isEqualTo("user-worker-in-app-1");
                    assertThat(inbox.getTitle()).isNotBlank();
                    assertThat(inbox.getMessage()).isNotBlank();
                    assertThat(inbox.getReadAt()).isNull();
                });
    }

    @Test
    @DisplayName("일시 실패가 발생하면 재시도 대기 상태와 다음 처리 가능 시간이 기록된다")
    void retryableFailureSchedulesNextRetryAndRecordsFailureReason() {
        emailDispatcher.nextResult(DispatchResult.retryableFailure("SMTP_TIMEOUT", "SMTP 서버 응답 지연"));
        CreateNotificationResponse response = notificationCommandService.createNotification(
                new CreateNotificationRequest(
                        "user-worker-retry-1",
                        NotificationType.PAYMENT_CONFIRMED,
                        "payment-worker-retry-1",
                        DispatchChannel.EMAIL,
                        Map.of("paymentId", "payment-worker-retry-1")
                )
        );

        int processedCount = notificationWorkerService.processDueDeliveries();

        NotificationDelivery delivery = notificationDeliveryRepository
                .findByNotificationRequestId(response.notificationId())
                .get(0);
        assertThat(processedCount).isEqualTo(1);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.RETRY_WAITING);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.getAvailableAt()).isEqualTo(WORKER_NOW.plusSeconds(2));
        assertThat(delivery.getLastFailureType()).isEqualTo(FailureType.RETRYABLE);
        assertThat(delivery.getLastFailureCode()).isEqualTo("SMTP_TIMEOUT");
        assertThat(delivery.getLastFailureMessage()).isEqualTo("SMTP 서버 응답 지연");
        assertThat(delivery.getLockOwner()).isNull();
        assertThat(delivery.getLockedUntil()).isNull();

        assertThat(notificationAttemptRepository.findByNotificationDeliveryIdOrderByAttemptNoAsc(delivery.getId()))
                .singleElement()
                .satisfies(attempt -> {
                    assertThat(attempt.getAttemptNo()).isEqualTo(1);
                    assertThat(attempt.getResult()).isEqualTo(AttemptResult.RETRYABLE_FAILED);
                    assertThat(attempt.getFailureType()).isEqualTo(FailureType.RETRYABLE);
                    assertThat(attempt.getFailureCode()).isEqualTo("SMTP_TIMEOUT");
                    assertThat(attempt.getFinishedAt()).isEqualTo(WORKER_NOW);
                });
    }

    @Test
    @DisplayName("일시 실패가 반복되면 지수 백오프 방식으로 다음 처리 가능 시간이 늘어난다")
    void repeatedRetryableFailureAppliesExponentialBackoff() {
        emailDispatcher.nextResult(DispatchResult.retryableFailure("SMTP_TIMEOUT", "첫 번째 일시 실패"));
        emailDispatcher.nextResult(DispatchResult.retryableFailure("SMTP_TIMEOUT", "두 번째 일시 실패"));
        CreateNotificationResponse response = notificationCommandService.createNotification(
                new CreateNotificationRequest(
                        "user-worker-exponential-1",
                        NotificationType.PAYMENT_CONFIRMED,
                        "payment-worker-exponential-1",
                        DispatchChannel.EMAIL,
                        Map.of("paymentId", "payment-worker-exponential-1")
                )
        );

        notificationWorkerService.processDueDeliveries();
        NotificationDelivery firstFailedDelivery = notificationDeliveryRepository
                .findByNotificationRequestId(response.notificationId())
                .get(0);
        assertThat(firstFailedDelivery.getAttemptCount()).isEqualTo(1);
        assertThat(firstFailedDelivery.getAvailableAt()).isEqualTo(WORKER_NOW.plusSeconds(2));

        firstFailedDelivery.markRetryWaiting(
                FailureType.RETRYABLE,
                "SMTP_TIMEOUT",
                "테스트에서 두 번째 시도를 즉시 실행하기 위해 대기 시간을 앞당깁니다",
                WORKER_NOW.minusSeconds(1)
        );
        notificationDeliveryRepository.saveAndFlush(firstFailedDelivery);

        notificationWorkerService.processDueDeliveries();

        NotificationDelivery secondFailedDelivery = notificationDeliveryRepository
                .findByNotificationRequestId(response.notificationId())
                .get(0);
        assertThat(secondFailedDelivery.getStatus()).isEqualTo(DeliveryStatus.RETRY_WAITING);
        assertThat(secondFailedDelivery.getAttemptCount()).isEqualTo(2);
        assertThat(secondFailedDelivery.getAvailableAt()).isEqualTo(WORKER_NOW.plusSeconds(4));
        assertThat(notificationAttemptRepository.findByNotificationDeliveryIdOrderByAttemptNoAsc(secondFailedDelivery.getId()))
                .hasSize(2)
                .extracting("attemptNo")
                .containsExactly(1, 2);
    }

    @Test
    @DisplayName("일시 실패라도 최대 시도 횟수에 도달하면 최종 실패 상태가 된다")
    void retryableFailureBecomesFailedWhenMaxAttemptsIsReached() {
        emailDispatcher.nextResult(DispatchResult.retryableFailure("SMTP_BUSY", "SMTP 서버가 계속 바쁩니다"));
        NotificationRequest request = notificationRequestRepository.saveAndFlush(
                NotificationRequest.create(
                        "user-worker-exhausted-1",
                        NotificationType.PAYMENT_CONFIRMED,
                        "payment-worker-exhausted-1",
                        DispatchChannel.EMAIL,
                        "{}"
                )
        );
        NotificationDelivery savedDelivery = notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.createPending(
                        request,
                        DeliveryChannel.EMAIL,
                        1,
                        WORKER_NOW
                )
        );

        int processedCount = notificationWorkerService.processDueDeliveries();

        NotificationDelivery delivery = notificationDeliveryRepository.findById(savedDelivery.getId()).orElseThrow();
        assertThat(processedCount).isEqualTo(1);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.getFailedAt()).isEqualTo(WORKER_NOW);
        assertThat(delivery.getLastFailureType()).isEqualTo(FailureType.RETRYABLE);
        assertThat(delivery.getLastFailureCode()).isEqualTo("SMTP_BUSY");

        assertThat(notificationAttemptRepository.findByNotificationDeliveryIdOrderByAttemptNoAsc(delivery.getId()))
                .singleElement()
                .satisfies(attempt -> {
                    assertThat(attempt.getResult()).isEqualTo(AttemptResult.RETRYABLE_FAILED);
                    assertThat(attempt.getFailureType()).isEqualTo(FailureType.RETRYABLE);
                });
    }

    @Test
    @DisplayName("영구 실패가 발생하면 재시도하지 않고 최종 실패 상태가 된다")
    void permanentFailureBecomesFailedWithoutRetry() {
        emailDispatcher.nextResult(DispatchResult.permanentFailure("INVALID_EMAIL", "수신자 이메일 주소가 올바르지 않습니다"));
        CreateNotificationResponse response = notificationCommandService.createNotification(
                new CreateNotificationRequest(
                        "user-worker-permanent-1",
                        NotificationType.ENROLLMENT_COMPLETED,
                        "enrollment-worker-permanent-1",
                        DispatchChannel.EMAIL,
                        Map.of("courseId", "course-worker-permanent-1")
                )
        );

        int processedCount = notificationWorkerService.processDueDeliveries();

        NotificationDelivery delivery = notificationDeliveryRepository
                .findByNotificationRequestId(response.notificationId())
                .get(0);
        assertThat(processedCount).isEqualTo(1);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.getFailedAt()).isEqualTo(WORKER_NOW);
        assertThat(delivery.getLastFailureType()).isEqualTo(FailureType.PERMANENT);
        assertThat(delivery.getLastFailureCode()).isEqualTo("INVALID_EMAIL");

        assertThat(notificationAttemptRepository.findByNotificationDeliveryIdOrderByAttemptNoAsc(delivery.getId()))
                .singleElement()
                .satisfies(attempt -> {
                    assertThat(attempt.getResult()).isEqualTo(AttemptResult.PERMANENT_FAILED);
                    assertThat(attempt.getFailureType()).isEqualTo(FailureType.PERMANENT);
                });
    }

    @Test
    @DisplayName("Dispatcher 예외는 무시하지 않고 재시도 가능한 실패로 기록된다")
    void dispatcherExceptionIsRecordedAsRetryableFailure() {
        emailDispatcher.nextException(new IllegalStateException("SMTP connection refused"));
        CreateNotificationResponse response = notificationCommandService.createNotification(
                new CreateNotificationRequest(
                        "user-worker-exception-1",
                        NotificationType.PAYMENT_CONFIRMED,
                        "payment-worker-exception-1",
                        DispatchChannel.EMAIL,
                        Map.of("paymentId", "payment-worker-exception-1")
                )
        );

        int processedCount = notificationWorkerService.processDueDeliveries();

        NotificationDelivery delivery = notificationDeliveryRepository
                .findByNotificationRequestId(response.notificationId())
                .get(0);
        assertThat(processedCount).isEqualTo(1);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.RETRY_WAITING);
        assertThat(delivery.getLastFailureType()).isEqualTo(FailureType.RETRYABLE);
        assertThat(delivery.getLastFailureCode()).isEqualTo("DISPATCH_EXCEPTION");
        assertThat(delivery.getLastFailureMessage()).isEqualTo("SMTP connection refused");

        assertThat(notificationAttemptRepository.findByNotificationDeliveryIdOrderByAttemptNoAsc(delivery.getId()))
                .singleElement()
                .satisfies(attempt -> {
                    assertThat(attempt.getResult()).isEqualTo(AttemptResult.RETRYABLE_FAILED);
                    assertThat(attempt.getFailureCode()).isEqualTo("DISPATCH_EXCEPTION");
                });
    }

    @TestConfiguration
    static class WorkerTestConfiguration {

        @Bean
        Clock clock() {
            return Clock.fixed(
                    WORKER_NOW.atZone(ClockConfig.KOREA_ZONE_ID).toInstant(),
                    ClockConfig.KOREA_ZONE_ID
            );
        }

        @Bean(name = "emailDispatcher")
        TestEmailDispatcher emailDispatcher() {
            return new TestEmailDispatcher();
        }
    }

    static class TestEmailDispatcher implements ChannelDispatcher {

        private final Queue<DispatchResult> results = new ConcurrentLinkedQueue<>();
        private final Queue<RuntimeException> exceptions = new ConcurrentLinkedQueue<>();

        @Override
        public DeliveryChannel channel() {
            return DeliveryChannel.EMAIL;
        }

        @Override
        public DispatchResult dispatch(NotificationDelivery delivery, RenderedMessage message) {
            RuntimeException exception = exceptions.poll();
            if (exception != null) {
                throw exception;
            }
            DispatchResult result = results.poll();
            if (result != null) {
                return result;
            }
            return DispatchResult.success();
        }

        void nextResult(DispatchResult result) {
            results.add(result);
        }

        void nextException(RuntimeException exception) {
            exceptions.add(exception);
        }

        void reset() {
            results.clear();
            exceptions.clear();
        }
    }
}
