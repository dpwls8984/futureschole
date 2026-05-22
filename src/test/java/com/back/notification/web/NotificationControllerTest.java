package com.back.notification.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.notification.infrastructure.persistence.NotificationAttemptRepository;
import com.back.notification.infrastructure.persistence.NotificationDeliveryRepository;
import com.back.notification.infrastructure.persistence.NotificationInboxRepository;
import com.back.notification.infrastructure.persistence.NotificationRequestRepository;
import com.back.notification.web.dto.CreateNotificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
    private NotificationInboxRepository notificationInboxRepository;

    @BeforeEach
    void setUp() {
        notificationInboxRepository.deleteAll();
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
                  "notificationType": "COURSE_CANCELED",
                  "eventId": "course-cancel-1",
                  "channel": "EMAIL",
                  "referenceData": {
                    "courseId": "course-1"
                  }
                }
                """;
        String inAppBody = """
                {
                  "recipientId": "user-1001",
                  "notificationType": "COURSE_CANCELED",
                  "eventId": "course-cancel-1",
                  "channel": "IN_APP",
                  "referenceData": {
                    "courseId": "course-1"
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
}
