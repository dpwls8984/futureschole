package com.back.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.notification.infrastructure.persistence.NotificationAttemptRepository;
import com.back.notification.infrastructure.persistence.NotificationDeliveryRepository;
import com.back.notification.infrastructure.persistence.NotificationInboxRepository;
import com.back.notification.infrastructure.persistence.NotificationRequestRepository;
import com.back.notification.service.NotificationWorkerService;
import com.fasterxml.jackson.databind.JsonNode;
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
@DisplayName("알림 시스템 통합 흐름")
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationWorkerService notificationWorkerService;

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
    @DisplayName("인앱 알림은 요청 등록, 비동기 처리, 목록 조회, 화면 알림 읽음 처리까지 이어진다")
    void inAppNotificationEndToEndFlow() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recipientId", "user-integration-1",
                                "notificationType", "COURSE_STARTING_TOMORROW",
                                "eventId", "course-integration-1",
                                "channel", "IN_APP",
                                "referenceData", Map.of("courseId", "course-integration-1")
                        ))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.deliveries[0].status").value("PENDING"))
                .andReturn();

        long notificationId = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("notificationId")
                .asLong();

        int processedCount = notificationWorkerService.processDueDeliveries();
        assertThat(processedCount).isEqualTo(1);

        mockMvc.perform(get("/notifications/{notificationId}", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.deliveries[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.deliveries[0].attemptCount").value(1));

        MvcResult inboxResult = mockMvc.perform(get("/me/notifications")
                        .header("X-User-Id", "user-integration-1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].notificationId").value(notificationId))
                .andExpect(jsonPath("$.content[0].eventId").value("course-integration-1"))
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andReturn();

        JsonNode inbox = objectMapper
                .readTree(inboxResult.getResponse().getContentAsString())
                .get("content")
                .get(0);
        long inboxId = inbox.get("inboxId").asLong();

        mockMvc.perform(patch("/me/notifications/read")
                        .header("X-User-Id", "user-integration-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("inboxIds", java.util.List.of(inboxId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(1))
                .andExpect(jsonPath("$.markedCount").value(1));

        mockMvc.perform(get("/me/notifications")
                        .header("X-User-Id", "user-integration-1")
                        .param("readStatus", "READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].read").value(true))
                .andExpect(jsonPath("$.content[0].readAt").isNotEmpty());
    }

    @Test
    @DisplayName("이메일 알림은 요청 등록 후 Worker가 발송 상태를 성공으로 확정한다")
    void emailNotificationEndToEndFlow() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recipientId", "user-integration-2",
                                "notificationType", "PAYMENT_CONFIRMED",
                                "eventId", "payment-integration-1",
                                "channel", "EMAIL",
                                "referenceData", Map.of("paymentId", "payment-integration-1")
                        ))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        long notificationId = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("notificationId")
                .asLong();

        int processedCount = notificationWorkerService.processDueDeliveries();
        assertThat(processedCount).isEqualTo(1);

        mockMvc.perform(get("/notifications/{notificationId}", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.deliveries[0].channel").value("EMAIL"))
                .andExpect(jsonPath("$.deliveries[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.deliveries[0].attemptCount").value(1));

        assertThat(notificationInboxRepository.count()).isZero();
    }
}
