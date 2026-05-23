package com.back.notification.web;

import com.back.global.exception.ErrorResponse;
import com.back.notification.enums.InboxReadStatus;
import com.back.notification.web.dto.InboxNotificationListResponse;
import com.back.notification.web.dto.MarkNotificationsReadRequest;
import com.back.notification.web.dto.MarkReadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "사용자 알림함 API", description = "사용자에게 노출되는 인앱 알림함 조회 API")
public interface NotificationInboxControllerSpec {

    @Operation(
            summary = "내 인앱 알림 목록 조회",
            description = """
                    현재 사용자의 인앱 알림함 목록을 조회합니다.

                    * 별도 알림 페이지뿐 아니라 알림함 버튼/드롭다운을 열 때 사용할 수 있는 API입니다.
                    * 과제의 간략 인증 조건에 맞춰 `X-User-Id` 헤더를 현재 사용자 식별자로 사용합니다.
                    * 조회 대상은 `notification_inboxes`이므로 EMAIL 알림은 목록에 포함되지 않습니다.
                    * 읽음 처리는 이 GET API에서 수행하지 않습니다. 알림함을 여는 순간 읽음 처리하는 UX는 별도 PATCH API에서 담당합니다.
                    * 정렬은 `visibleAt desc, inboxId desc`입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = InboxNotificationListResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "recipientId": "user-1001",
                                      "readStatus": "ALL",
                                      "page": 0,
                                      "size": 20,
                                      "totalElements": 1,
                                      "totalPages": 1,
                                      "hasNext": false,
                                      "content": [
                                        {
                                          "inboxId": 1,
                                          "notificationId": 10,
                                          "deliveryId": 15,
                                          "notificationType": "COURSE_STARTING_TOMORROW",
                                          "eventId": "course-start-20260523-1",
                                          "title": "강의 시작 알림",
                                          "message": "내일 수강 예정인 강의가 시작됩니다.",
                                          "referenceData": {
                                            "courseId": "course-3001"
                                          },
                                          "read": false,
                                          "visibleAt": "2026-05-23T10:20:00",
                                          "readAt": null,
                                          "createdAt": "2026-05-23T10:20:00"
                                        }
                                      ]
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "헤더 또는 파라미터 오류",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "INVALID_INPUT_VALUE",
                                        "status": "400",
                                        "message": "유효하지 않은 입력 값입니다."
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping
    ResponseEntity<InboxNotificationListResponse> getMyNotifications(
            @Parameter(
                    name = "X-User-Id",
                    description = "현재 사용자 ID. 과제용 간략 인증 식별자로 사용합니다.",
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "user-1001"
            )
            @RequestHeader("X-User-Id") String currentUserId,

            @Parameter(description = "읽음 상태 필터", example = "ALL")
            @RequestParam(defaultValue = "ALL") InboxReadStatus readStatus,

            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기. 1 이상 50 이하", example = "20")
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(
            summary = "현재 화면의 인앱 알림 읽음 처리",
            description = """
                    현재 사용자가 실제 화면에서 받은 인앱 알림만 읽음 처리합니다.

                    * 전체 안읽음 알림을 일괄 처리하지 않습니다.
                    * 클라이언트는 직전에 조회해서 화면에 렌더링한 `inboxId` 목록만 전달합니다.
                    * 서버는 `inboxId`만 믿지 않고 `X-User-Id`와 함께 조건을 걸어 현재 사용자 소유 알림만 변경합니다.
                    * 이미 읽은 알림은 다시 변경하지 않으므로 같은 요청을 반복해도 멱등하게 동작합니다.
                    * 다음 페이지에 있어 아직 화면에 보이지 않은 알림은 요청 body에 포함되지 않으므로 안읽음 상태로 남습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "읽음 처리 완료",
                    content = @Content(
                            schema = @Schema(implementation = MarkReadResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "recipientId": "user-1001",
                                      "requestedCount": 10,
                                      "markedCount": 10,
                                      "readAt": "2026-05-23T10:30:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "헤더 또는 요청 본문 오류",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "INVALID_INPUT_VALUE",
                                        "status": "400",
                                        "message": "유효하지 않은 입력 값입니다."
                                      }
                                    }
                                    """)
                    )
            )
    })
    @PatchMapping("/read")
    ResponseEntity<MarkReadResponse> markVisibleNotificationsRead(
            @Parameter(
                    name = "X-User-Id",
                    description = "현재 사용자 ID. 과제용 간략 인증 식별자로 사용합니다.",
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "user-1001"
            )
            @RequestHeader("X-User-Id") String currentUserId,

            @Valid @RequestBody MarkNotificationsReadRequest request
    );
}
