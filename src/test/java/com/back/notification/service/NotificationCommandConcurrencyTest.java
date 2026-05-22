package com.back.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.notification.enums.DispatchChannel;
import com.back.notification.enums.NotificationType;
import com.back.notification.infrastructure.persistence.NotificationAttemptRepository;
import com.back.notification.infrastructure.persistence.NotificationDeliveryRepository;
import com.back.notification.infrastructure.persistence.NotificationInboxRepository;
import com.back.notification.infrastructure.persistence.NotificationRequestRepository;
import com.back.notification.web.dto.CreateNotificationRequest;
import com.back.notification.web.dto.CreateNotificationResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("알림 요청 등록 동시성")
class NotificationCommandConcurrencyTest {

    private static final int THREAD_COUNT = 20;

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

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        notificationInboxRepository.deleteAll();
        notificationAttemptRepository.deleteAll();
        notificationDeliveryRepository.deleteAll();
        notificationRequestRepository.deleteAll();
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    @DisplayName("같은 요청 20개가 동시에 들어와도 요청과 delivery는 하나만 생성된다")
    void sameNotificationRequestConcurrentlyCreatesOnlyOneRequestAndDelivery() throws Exception {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "user-concurrent-1",
                NotificationType.PAYMENT_CONFIRMED,
                "payment-concurrent-1",
                DispatchChannel.EMAIL,
                Map.of("paymentId", "payment-concurrent-1")
        );

        List<CreateNotificationResponse> responses = runConcurrently(
                THREAD_COUNT,
                () -> notificationCommandService.createNotification(request)
        );

        Set<Long> notificationIds = responses.stream()
                .map(CreateNotificationResponse::notificationId)
                .collect(Collectors.toSet());

        assertThat(notificationIds).hasSize(1);
        assertThat(responses).filteredOn(CreateNotificationResponse::duplicated).hasSize(THREAD_COUNT - 1);
        assertThat(responses).filteredOn(response -> !response.duplicated()).hasSize(1);
        assertThat(notificationRequestRepository.count()).isEqualTo(1);
        assertThat(notificationDeliveryRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("동시 중복 요청에서 unique constraint 충돌이 발생해도 500이 아니라 기존 요청으로 처리된다")
    void duplicateRequestUniqueConstraintViolationReturnsExistingRequestWithoutServerError() throws Exception {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "user-concurrent-2",
                NotificationType.ENROLLMENT_COMPLETED,
                "enrollment-concurrent-1",
                DispatchChannel.IN_APP,
                Map.of("courseId", "course-concurrent-1")
        );

        List<CreateNotificationResponse> responses = runConcurrently(
                THREAD_COUNT,
                () -> notificationCommandService.createNotification(request)
        );

        assertThat(responses).hasSize(THREAD_COUNT);
        assertThat(responses)
                .extracting(CreateNotificationResponse::notificationId)
                .containsOnly(responses.get(0).notificationId());
        assertThat(responses).filteredOn(CreateNotificationResponse::duplicated).hasSize(THREAD_COUNT - 1);
        assertThat(notificationRequestRepository.count()).isEqualTo(1);
        assertThat(notificationDeliveryRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 dedup key가 서로 다른 채널로 동시에 요청되면 한 채널만 유지되고 나머지는 conflict 처리된다")
    void sameDedupKeyWithDifferentChannelsConcurrentlyKeepsOnlyOneChannel() throws Exception {
        List<Callable<ConcurrentChannelResult>> tasks = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            DispatchChannel channel = i % 2 == 0 ? DispatchChannel.EMAIL : DispatchChannel.IN_APP;
            tasks.add(() -> createNotificationOrConflict(channel));
        }

        List<ConcurrentChannelResult> results = runConcurrently(tasks);
        List<ConcurrentChannelResult> successes = results.stream()
                .filter(result -> !result.conflict())
                .toList();
        List<ConcurrentChannelResult> conflicts = results.stream()
                .filter(ConcurrentChannelResult::conflict)
                .toList();

        assertThat(successes).isNotEmpty();
        assertThat(conflicts).isNotEmpty();
        assertThat(successes)
                .extracting(ConcurrentChannelResult::notificationId)
                .containsOnly(successes.get(0).notificationId());
        assertThat(successes)
                .extracting(ConcurrentChannelResult::channel)
                .containsOnly(successes.get(0).channel());
        assertThat(notificationRequestRepository.count()).isEqualTo(1);
        assertThat(notificationDeliveryRepository.count()).isEqualTo(1);
    }

    private ConcurrentChannelResult createNotificationOrConflict(DispatchChannel channel) {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "user-concurrent-3",
                NotificationType.COURSE_CANCELED,
                "course-cancel-concurrent-1",
                channel,
                Map.of("courseId", "course-concurrent-3")
        );

        try {
            CreateNotificationResponse response = notificationCommandService.createNotification(request);
            return ConcurrentChannelResult.success(response.notificationId(), response.requestedChannel());
        } catch (ServiceException e) {
            if (e.getErrorCode() == ErrorCode.NOTIFICATION_CHANNEL_CONFLICT) {
                return ConcurrentChannelResult.conflict(channel);
            }
            throw e;
        }
    }

    private <T> List<T> runConcurrently(int count, Callable<T> task) throws Exception {
        List<Callable<T>> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(task);
        }
        return runConcurrently(tasks);
    }

    private <T> List<T> runConcurrently(List<Callable<T>> tasks) throws Exception {
        CountDownLatch readyLatch = new CountDownLatch(tasks.size());
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();

        for (Callable<T> task : tasks) {
            futures.add(executorService.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                return task.call();
            }));
        }

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        startLatch.countDown();

        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get(10, TimeUnit.SECONDS));
        }
        return results;
    }

    private record ConcurrentChannelResult(
            Long notificationId,
            DispatchChannel channel,
            boolean conflict
    ) {

        static ConcurrentChannelResult success(Long notificationId, DispatchChannel channel) {
            return new ConcurrentChannelResult(notificationId, channel, false);
        }

        static ConcurrentChannelResult conflict(DispatchChannel channel) {
            return new ConcurrentChannelResult(null, channel, true);
        }
    }
}
