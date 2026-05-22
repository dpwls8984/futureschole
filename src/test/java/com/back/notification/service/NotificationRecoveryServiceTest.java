package com.back.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.global.config.ClockConfig;
import com.back.notification.domain.NotificationDelivery;
import com.back.notification.domain.NotificationRequest;
import com.back.notification.enums.DeliveryChannel;
import com.back.notification.enums.DeliveryStatus;
import com.back.notification.enums.DispatchChannel;
import com.back.notification.enums.NotificationType;
import com.back.notification.infrastructure.persistence.NotificationAttemptRepository;
import com.back.notification.infrastructure.persistence.NotificationDeliveryRepository;
import com.back.notification.infrastructure.persistence.NotificationInboxRepository;
import com.back.notification.infrastructure.persistence.NotificationRequestRepository;
import java.time.Clock;
import java.time.LocalDateTime;
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
@Import(NotificationRecoveryServiceTest.RecoveryTestConfiguration.class)
@DisplayName("알림 발송 작업 복구")
class NotificationRecoveryServiceTest {

    private static final LocalDateTime RECOVERY_NOW = LocalDateTime.of(2026, 5, 22, 11, 0);

    @Autowired
    private NotificationRecoveryService notificationRecoveryService;

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
    }

    @Test
    @DisplayName("처리 중 상태에서 lock 시간이 지난 발송 작업은 다시 대기 상태로 복구된다")
    void recoverExpiredProcessingDeliveryToPending() {
        NotificationDelivery delivery = saveProcessingDelivery(
                "user-recovery-1",
                "payment-recovery-1",
                RECOVERY_NOW.minusSeconds(1)
        );

        int recoveredCount = notificationRecoveryService.recoverStuckDeliveries();

        NotificationDelivery recoveredDelivery = notificationDeliveryRepository.findById(delivery.getId())
                .orElseThrow();
        assertThat(recoveredCount).isEqualTo(1);
        assertThat(recoveredDelivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(recoveredDelivery.getAvailableAt()).isEqualTo(RECOVERY_NOW);
        assertThat(recoveredDelivery.getLockOwner()).isNull();
        assertThat(recoveredDelivery.getLockedUntil()).isNull();
        assertThat(recoveredDelivery.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("처리 중이어도 lock 시간이 남아 있으면 복구하지 않는다")
    void doesNotRecoverProcessingDeliveryBeforeLockExpires() {
        NotificationDelivery delivery = saveProcessingDelivery(
                "user-recovery-2",
                "payment-recovery-2",
                RECOVERY_NOW.plusSeconds(30)
        );

        int recoveredCount = notificationRecoveryService.recoverStuckDeliveries();

        NotificationDelivery processingDelivery = notificationDeliveryRepository.findById(delivery.getId())
                .orElseThrow();
        assertThat(recoveredCount).isZero();
        assertThat(processingDelivery.getStatus()).isEqualTo(DeliveryStatus.PROCESSING);
        assertThat(processingDelivery.getLockOwner()).isEqualTo("stuck-worker");
        assertThat(processingDelivery.getLockedUntil()).isEqualTo(RECOVERY_NOW.plusSeconds(30));
        assertThat(processingDelivery.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 단말 상태인 발송 작업은 lock 시간이 지났더라도 복구하지 않는다")
    void doesNotRecoverTerminalDelivery() {
        NotificationRequest request = saveRequest("user-recovery-3", "payment-recovery-3");
        NotificationDelivery delivery = NotificationDelivery.createPending(
                request,
                DeliveryChannel.EMAIL,
                5,
                RECOVERY_NOW
        );
        delivery.claim("finished-worker", RECOVERY_NOW.minusSeconds(1));
        delivery.markSucceeded(RECOVERY_NOW.minusSeconds(2));
        NotificationDelivery savedDelivery = notificationDeliveryRepository.saveAndFlush(delivery);

        int recoveredCount = notificationRecoveryService.recoverStuckDeliveries();

        NotificationDelivery terminalDelivery = notificationDeliveryRepository.findById(savedDelivery.getId())
                .orElseThrow();
        assertThat(recoveredCount).isZero();
        assertThat(terminalDelivery.getStatus()).isEqualTo(DeliveryStatus.SUCCEEDED);
        assertThat(terminalDelivery.getAttemptCount()).isEqualTo(1);
        assertThat(terminalDelivery.getSucceededAt()).isEqualTo(RECOVERY_NOW.minusSeconds(2));
    }

    private NotificationDelivery saveProcessingDelivery(
            String recipientId,
            String eventId,
            LocalDateTime lockedUntil
    ) {
        NotificationRequest request = saveRequest(recipientId, eventId);
        NotificationDelivery delivery = NotificationDelivery.createPending(
                request,
                DeliveryChannel.EMAIL,
                5,
                RECOVERY_NOW
        );
        delivery.claim("stuck-worker", lockedUntil);
        return notificationDeliveryRepository.saveAndFlush(delivery);
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

    @TestConfiguration
    static class RecoveryTestConfiguration {

        @Bean
        Clock clock() {
            return Clock.fixed(
                    RECOVERY_NOW.atZone(ClockConfig.KOREA_ZONE_ID).toInstant(),
                    ClockConfig.KOREA_ZONE_ID
            );
        }
    }
}
