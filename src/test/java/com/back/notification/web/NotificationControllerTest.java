package com.back.notification.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.notification.domain.NotificationDelivery;
import com.back.notification.domain.NotificationRequest;
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
import com.back.notification.web.dto.CreateNotificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("알림 요청 등록 API")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private void deleteAll() {
        notificationInboxRepository.deleteAll();
        notificationManualRetryRepository.deleteAll();
        notificationAttemptRepository.deleteAll();
        notificationDeliveryRepository.deleteAll();
        notificationRequestRepository.deleteAll();
    }

    @Test
    @DisplayName("신규 알림 요청을 등록하면 202 Accepted와 PENDING delivery를 반환한다")
    void createNotificationReturnsAcceptedAndCreatesRequestAndDelivery() throws Exception {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "user-1001",
                com.back.notification.enums.NotificationType.PAYMENT_CONFIRMED,
                "payment-20260522-0001",
                com.back.notification.enums.DispatchChannel.EMAIL,
                Map.of("paymentId", "pay-9001", "courseId", "course-3001")
        );

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.notificationId").isNumber())
                .andExpect(jsonPath("$.recipientId").value("user-1001"))
                .andExpect(jsonPath("$.notificationType").value("PAYMENT_CONFIRMED"))
                .andExpect(jsonPath("$.eventId").value("payment-20260522-0001"))
                .andExpect(jsonPath("$.requestedChannel").value("EMAIL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.duplicated").value(false))
                .andExpect(jsonPath("$.deliveries[0].deliveryId").isNumber())
                .andExpect(jsonPath("$.deliveries[0].channel").value("EMAIL"))
                .andExpect(jsonPath("$.deliveries[0].status").value("PENDING"));

        assertThat(notificationRequestRepository.count()).isEqualTo(1);
        assertThat(notificationDeliveryRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("동일한 알림 요청이 다시 들어오면 기존 알림을 200 OK로 반환한다")
    void createNotificationReturnsExistingRequestForSameDuplicateRequest() throws Exception {
        String body = """
                {
                  "recipientId": "user-1001",
                  "notificationType": "ENROLLMENT_COMPLETED",
                  "eventId": "enrollment-1",
                  "channel": "IN_APP",
                  "referenceData": {
                    "courseId": "course-1"
                  }
                }
                """;

        MvcResult firstResult = mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andReturn();

        Long firstNotificationId = objectMapper
                .readTree(firstResult.getResponse().getContentAsString())
                .get("notificationId")
                .asLong();

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(firstNotificationId))
                .andExpect(jsonPath("$.duplicated").value(true));

        assertThat(notificationRequestRepository.count()).isEqualTo(1);
        assertThat(notificationDeliveryRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("동일 이벤트 알림이 다른 채널로 다시 요청되면 409 Conflict를 반환한다")
    void createNotificationReturnsConflictWhenSameEventRequestedWithDifferentChannel() throws Exception {
        String emailBody = """
                {
                  "recipientId": "user-1001",
                  "notificationType": "PAYMENT_CANCELED",
                  "eventId": "payment-cancel-1",
                  "channel": "EMAIL",
                  "referenceData": {
                    "paymentId": "payment-1"
                  }
                }
                """;
        String inAppBody = """
                {
                  "recipientId": "user-1001",
                  "notificationType": "PAYMENT_CANCELED",
                  "eventId": "payment-cancel-1",
                  "channel": "IN_APP",
                  "referenceData": {
                    "paymentId": "payment-1"
                  }
                }
                """;

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailBody))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inAppBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NOTIFICATION_CHANNEL_CONFLICT"));

        assertThat(notificationRequestRepository.count()).isEqualTo(1);
        assertThat(notificationDeliveryRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("알림 요청 ID로 현재 처리 상태를 조회한다")
    void getNotificationDetailReturnsNotificationStatus() throws Exception {
        String body = """
                {
                  "recipientId": "user-1001",
                  "notificationType": "PAYMENT_CONFIRMED",
                  "eventId": "payment-detail-1",
                  "channel": "EMAIL",
                  "referenceData": {
                    "paymentId": "pay-detail-1",
                    "courseId": "course-detail-1"
                  }
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andReturn();

        Long notificationId = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("notificationId")
                .asLong();

        mockMvc.perform(get("/notifications/{notificationId}", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(notificationId))
                .andExpect(jsonPath("$.recipientId").value("user-1001"))
                .andExpect(jsonPath("$.notificationType").value("PAYMENT_CONFIRMED"))
                .andExpect(jsonPath("$.eventId").value("payment-detail-1"))
                .andExpect(jsonPath("$.requestedChannel").value("EMAIL"))
                .andExpect(jsonPath("$.referenceData.paymentId").value("pay-detail-1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.deliveries[0].channel").value("EMAIL"))
                .andExpect(jsonPath("$.deliveries[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("존재하지 않는 알림 요청 ID로 상태 조회를 하면 404를 반환한다")
    void getNotificationDetailReturnsNotFoundForMissingNotification() throws Exception {
        mockMvc.perform(get("/notifications/{notificationId}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("최종 실패 알림을 수동 재시도하면 발송 작업이 새 재시도 사이클의 PENDING 상태가 된다")
    void retryFailedNotificationResetsDeliveryForManualRetry() throws Exception {
        NotificationRequest request = notificationRequestRepository.saveAndFlush(
                NotificationRequest.create(
                        "user-manual-api-1",
                        NotificationType.PAYMENT_CONFIRMED,
                        "payment-manual-api-1",
                        DispatchChannel.EMAIL,
                        "{}"
                )
        );
        NotificationDelivery delivery = NotificationDelivery.createPending(
                request,
                DeliveryChannel.EMAIL,
                1,
                LocalDateTime.of(2026, 5, 23, 12, 0)
        );
        delivery.claim("failed-worker", LocalDateTime.of(2026, 5, 23, 12, 1));
        delivery.markFailed(
                FailureType.RETRYABLE,
                "SMTP_BUSY",
                "SMTP 서버가 계속 바쁩니다",
                LocalDateTime.of(2026, 5, 23, 12, 2)
        );
        NotificationDelivery savedDelivery = notificationDeliveryRepository.saveAndFlush(delivery);

        mockMvc.perform(post("/notifications/{notificationId}/retry", request.getId())
                        .header("X-Admin-Id", "admin-api-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "외부 이메일 서버 장애 복구 후 재시도합니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(request.getId()))
                .andExpect(jsonPath("$.requestedBy").value("admin-api-1"))
                .andExpect(jsonPath("$.retriedDeliveryCount").value(1))
                .andExpect(jsonPath("$.deliveries[0].deliveryId").value(savedDelivery.getId()))
                .andExpect(jsonPath("$.deliveries[0].status").value("PENDING"))
                .andExpect(jsonPath("$.deliveries[0].retryCycle").value(1))
                .andExpect(jsonPath("$.deliveries[0].attemptCount").value(0))
                .andExpect(jsonPath("$.deliveries[0].manualRetryCount").value(1));

        NotificationDelivery retriedDelivery = notificationDeliveryRepository.findById(savedDelivery.getId())
                .orElseThrow();
        assertThat(retriedDelivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(retriedDelivery.getRetryCycle()).isEqualTo(1);
        assertThat(retriedDelivery.getAttemptCount()).isZero();
        assertThat(notificationManualRetryRepository.count()).isEqualTo(1);
    }
}
