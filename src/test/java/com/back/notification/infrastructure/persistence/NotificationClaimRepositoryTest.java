package com.back.notification.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.notification.domain.NotificationDelivery;
import com.back.notification.domain.NotificationRequest;
import com.back.notification.enums.DeliveryChannel;
import com.back.notification.enums.DeliveryStatus;
import com.back.notification.enums.DispatchChannel;
import com.back.notification.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("알림 발송 작업 처리 권한 선점")
class NotificationClaimRepositoryTest {

    @Autowired
    private NotificationClaimRepository notificationClaimRepository;

    @Autowired
    private NotificationRequestRepository notificationRequestRepository;

    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Autowired
    private NotificationAttemptRepository notificationAttemptRepository;

    @Autowired
    private NotificationInboxRepository notificationInboxRepository;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        notificationInboxRepository.deleteAll();
        notificationAttemptRepository.deleteAll();
        notificationDeliveryRepository.deleteAll();
        notificationRequestRepository.deleteAll();
        executorService = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    @DisplayName("두 Worker가 같은 알림 발송 작업을 동시에 가져가려 해도 하나만 처리 권한을 얻는다")
    void onlyOneWorkerCanClaimSameDelivery() throws Exception {
        NotificationRequest request = notificationRequestRepository.saveAndFlush(
                NotificationRequest.create(
                        "user-worker-1",
                        NotificationType.PAYMENT_CONFIRMED,
                        "payment-worker-1",
                        DispatchChannel.EMAIL,
                        "{}"
                )
        );
        NotificationDelivery delivery = notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.createPending(
                        request,
                        DeliveryChannel.EMAIL,
                        5,
                        LocalDateTime.of(2026, 5, 22, 10, 0)
                )
        );

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String workerId = "worker-" + i;
            futures.add(executorService.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                return notificationClaimRepository.claimDelivery(
                        delivery.getId(),
                        workerId,
                        LocalDateTime.of(2026, 5, 22, 10, 1),
                        LocalDateTime.of(2026, 5, 22, 10, 0)
                );
            }));
        }

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        startLatch.countDown();

        int affectedRows = 0;
        for (Future<Integer> future : futures) {
            affectedRows += future.get(10, TimeUnit.SECONDS);
        }

        NotificationDelivery claimedDelivery = notificationDeliveryRepository.findById(delivery.getId()).orElseThrow();

        assertThat(affectedRows).isEqualTo(1);
        assertThat(claimedDelivery.getStatus()).isEqualTo(DeliveryStatus.PROCESSING);
        assertThat(claimedDelivery.getAttemptCount()).isEqualTo(1);
        assertThat(claimedDelivery.getLockOwner()).startsWith("worker-");
    }
}
