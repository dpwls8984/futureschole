package com.back.notification.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.notification.domain.NotificationDelivery;
import com.back.notification.domain.NotificationInbox;
import com.back.notification.domain.NotificationRequest;
import com.back.notification.enums.DeliveryChannel;
import com.back.notification.enums.DispatchChannel;
import com.back.notification.enums.NotificationType;
import com.back.notification.infrastructure.persistence.NotificationAttemptRepository;
import com.back.notification.infrastructure.persistence.NotificationDeliveryRepository;
import com.back.notification.infrastructure.persistence.NotificationInboxRepository;
import com.back.notification.infrastructure.persistence.NotificationRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("사용자 인앱 알림 목록 조회 API")
class NotificationInboxControllerTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 22, 10, 0);

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
    @DisplayName("현재 사용자의 인앱 알림을 최신순으로 페이징 조회한다")
    void getMyNotificationsReturnsPagedInAppNotifications() throws Exception {
        saveInbox("user-me-1", "event-old", BASE_TIME.plusMinutes(1), false);
        saveInbox("user-me-1", "event-middle", BASE_TIME.plusMinutes(2), false);
        saveInbox("user-me-1", "event-new", BASE_TIME.plusMinutes(3), false);
        saveInbox("other-user", "event-other", BASE_TIME.plusMinutes(4), false);

        mockMvc.perform(get("/me/notifications")
                        .header("X-User-Id", "user-me-1")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientId").value("user-me-1"))
                .andExpect(jsonPath("$.readStatus").value("ALL"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.content[0].eventId").value("event-new"))
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andExpect(jsonPath("$.content[1].eventId").value("event-middle"));
    }

    @Test
    @DisplayName("읽음 상태 필터가 UNREAD이면 읽지 않은 인앱 알림만 조회한다")
    void getMyNotificationsWithUnreadFilterReturnsOnlyUnreadNotifications() throws Exception {
        saveInbox("user-me-2", "event-unread", BASE_TIME.plusMinutes(1), false);
        saveInbox("user-me-2", "event-read", BASE_TIME.plusMinutes(2), true);

        mockMvc.perform(get("/me/notifications")
                        .header("X-User-Id", "user-me-2")
                        .param("readStatus", "UNREAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readStatus").value("UNREAD"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].eventId").value("event-unread"))
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andExpect(jsonPath("$.content[0].readAt").doesNotExist());
    }

    @Test
    @DisplayName("읽음 상태 필터가 READ이면 읽은 인앱 알림만 조회한다")
    void getMyNotificationsWithReadFilterReturnsOnlyReadNotifications() throws Exception {
        saveInbox("user-me-3", "event-unread", BASE_TIME.plusMinutes(1), false);
        saveInbox("user-me-3", "event-read", BASE_TIME.plusMinutes(2), true);

        mockMvc.perform(get("/me/notifications")
                        .header("X-User-Id", "user-me-3")
                        .param("readStatus", "READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readStatus").value("READ"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].eventId").value("event-read"))
                .andExpect(jsonPath("$.content[0].read").value(true))
                .andExpect(jsonPath("$.content[0].readAt").isNotEmpty());
    }

    @Test
    @DisplayName("X-User-Id 헤더가 없으면 400을 반환한다")
    void getMyNotificationsReturnsBadRequestWhenUserHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/me/notifications"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("페이지 크기가 허용 범위를 넘으면 400을 반환한다")
    void getMyNotificationsReturnsBadRequestWhenPageSizeIsTooLarge() throws Exception {
        mockMvc.perform(get("/me/notifications")
                        .header("X-User-Id", "user-me-4")
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("현재 화면에 렌더링된 인앱 알림만 읽음 처리한다")
    void markReadUpdatesOnlyRenderedNotifications() throws Exception {
        List<NotificationInbox> inboxes = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            inboxes.add(saveInbox(
                    "user-read-1",
                    "event-read-page-" + i,
                    BASE_TIME.plusMinutes(i),
                    false
            ));
        }
        List<Long> renderedInboxIds = inboxes.subList(2, 12).stream()
                .map(NotificationInbox::getId)
                .toList();

        mockMvc.perform(patch("/me/notifications/read")
                        .header("X-User-Id", "user-read-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("inboxIds", renderedInboxIds))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientId").value("user-read-1"))
                .andExpect(jsonPath("$.requestedCount").value(10))
                .andExpect(jsonPath("$.markedCount").value(10))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        List<NotificationInbox> refreshedInboxes = notificationInboxRepository.findAllById(
                inboxes.stream().map(NotificationInbox::getId).toList()
        );
        assertThat(refreshedInboxes)
                .filteredOn(inbox -> renderedInboxIds.contains(inbox.getId()))
                .allSatisfy(inbox -> assertThat(inbox.getReadAt()).isNotNull());
        assertThat(refreshedInboxes)
                .filteredOn(inbox -> !renderedInboxIds.contains(inbox.getId()))
                .allSatisfy(inbox -> assertThat(inbox.getReadAt()).isNull());
    }

    @Test
    @DisplayName("다른 사용자의 inboxId가 섞여도 현재 사용자 알림만 읽음 처리한다")
    void markReadIgnoresOtherUsersNotifications() throws Exception {
        NotificationInbox myInbox = saveInbox("user-read-2", "event-my", BASE_TIME.plusMinutes(1), false);
        NotificationInbox otherInbox = saveInbox("other-user-read-2", "event-other", BASE_TIME.plusMinutes(2), false);

        mockMvc.perform(patch("/me/notifications/read")
                        .header("X-User-Id", "user-read-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "inboxIds",
                                List.of(myInbox.getId(), otherInbox.getId())
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(2))
                .andExpect(jsonPath("$.markedCount").value(1));

        NotificationInbox refreshedMyInbox = notificationInboxRepository.findById(myInbox.getId()).orElseThrow();
        NotificationInbox refreshedOtherInbox = notificationInboxRepository.findById(otherInbox.getId()).orElseThrow();
        assertThat(refreshedMyInbox.getReadAt()).isNotNull();
        assertThat(refreshedOtherInbox.getReadAt()).isNull();
    }

    @Test
    @DisplayName("같은 읽음 처리 요청을 반복하면 두 번째 요청은 변경 없이 0개 처리된다")
    void markReadIsIdempotent() throws Exception {
        NotificationInbox inbox = saveInbox("user-read-3", "event-idempotent", BASE_TIME.plusMinutes(1), false);
        String body = objectMapper.writeValueAsString(Map.of("inboxIds", List.of(inbox.getId())));

        mockMvc.perform(patch("/me/notifications/read")
                        .header("X-User-Id", "user-read-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedCount").value(1));

        mockMvc.perform(patch("/me/notifications/read")
                        .header("X-User-Id", "user-read-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(1))
                .andExpect(jsonPath("$.markedCount").value(0));
    }

    @Test
    @DisplayName("노출 전 인앱 알림은 읽음 처리하지 않는다")
    void markReadDoesNotUpdateInvisibleNotification() throws Exception {
        NotificationInbox inbox = saveInbox(
                "user-read-4",
                "event-invisible",
                LocalDateTime.of(2099, 1, 1, 0, 0),
                false
        );

        mockMvc.perform(patch("/me/notifications/read")
                        .header("X-User-Id", "user-read-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("inboxIds", List.of(inbox.getId())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(1))
                .andExpect(jsonPath("$.markedCount").value(0));

        NotificationInbox refreshedInbox = notificationInboxRepository.findById(inbox.getId()).orElseThrow();
        assertThat(refreshedInbox.getReadAt()).isNull();
    }

    @Test
    @DisplayName("읽음 처리 대상 목록이 비어 있으면 400을 반환한다")
    void markReadReturnsBadRequestWhenInboxIdsAreEmpty() throws Exception {
        mockMvc.perform(patch("/me/notifications/read")
                        .header("X-User-Id", "user-read-5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inboxIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    private NotificationInbox saveInbox(
            String recipientId,
            String eventId,
            LocalDateTime visibleAt,
            boolean read
    ) {
        NotificationRequest request = notificationRequestRepository.saveAndFlush(
                NotificationRequest.create(
                        recipientId,
                        NotificationType.COURSE_STARTING_TOMORROW,
                        eventId,
                        DispatchChannel.IN_APP,
                        """
                                {"courseId":"course-%s"}
                                """.formatted(eventId)
                )
        );
        NotificationDelivery delivery = notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.createPending(
                        request,
                        DeliveryChannel.IN_APP,
                        5,
                        visibleAt
                )
        );
        delivery.markSucceeded(visibleAt);

        NotificationInbox inbox = NotificationInbox.create(
                delivery,
                recipientId,
                "강의 시작 알림",
                "내일 수강 예정인 강의가 시작됩니다.",
                visibleAt
        );
        if (read) {
            inbox.markRead(visibleAt.plusMinutes(1));
        }
        NotificationInbox savedInbox = notificationInboxRepository.saveAndFlush(inbox);

        assertThat(savedInbox.getId()).isNotNull();
        return savedInbox;
    }
}
