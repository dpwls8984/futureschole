package com.back.notification.web;

import com.back.global.exception.ErrorResponse;
import com.back.notification.web.dto.CreateNotificationRequest;
import com.back.notification.web.dto.CreateNotificationResponse;
import com.back.notification.web.dto.NotificationDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "알림 API", description = "알림 요청 등록과 발송 상태 관리를 위한 API")
public interface NotificationControllerSpec {

    @Operation(
            summary = "알림 발송 요청 등록",
            description = """
                    알림 발송 요청을 접수합니다. 실제 발송은 즉시 수행하지 않고 Worker가 비동기로 처리합니다.

                    * **중복 기준:** `recipientId + notificationType + eventId`
                      * `recipientId`: 알림을 받을 사용자
                      * `notificationType`: 알림의 종류
                      * `eventId`: 알림을 발생시킨 원천 이벤트 ID
                      * 같은 사용자에게 같은 이벤트로 같은 타입의 알림이 이미 등록되어 있다면, 같은 알림 요청으로 간주합니다.
                      * 발송 채널은 중복 기준에 포함하지 않습니다. 요구사항의 "이메일 또는 인앱 알림"을 하나의 이벤트 알림은 하나의 채널로 발송된다고 해석했기 때문입니다.
                      * 중복 여부는 애플리케이션 조회만으로 판단하지 않고, DB의 unique constraint로 최종 보장합니다.
                    * **신규 요청:** `202 Accepted`
                    * **완전 중복 요청:** `200 OK`와 기존 알림 요청 반환
                    * **동일 이벤트/타입/수신자이지만 다른 채널:** `409 Conflict`
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "신규 알림 요청 접수 완료",
                    content = @Content(
                            schema = @Schema(implementation = CreateNotificationResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "notificationId": 1,
                                      "recipientId": "user-1001",
                                      "notificationType": "PAYMENT_CONFIRMED",
                                      "eventId": "payment-20260522-0001",
                                      "requestedChannel": "EMAIL",
                                      "status": "PENDING",
                                      "deliveries": [
                                        {
                                          "deliveryId": 1,
                                          "channel": "EMAIL",
                                          "status": "PENDING",
                                          "attemptCount": 0,
                                          "maxAttempts": 5,
                                          "availableAt": "2026-05-22T19:30:00",
                                          "lastFailureCode": null,
                                          "lastFailureMessage": null,
                                          "succeededAt": null,
                                          "failedAt": null
                                        }
                                      ],
                                      "duplicated": false,
                                      "createdAt": "2026-05-22T19:30:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "동일 알림 요청이 이미 존재하여 기존 요청 반환",
                    content = @Content(
                            schema = @Schema(implementation = CreateNotificationResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "notificationId": 1,
                                      "recipientId": "user-1001",
                                      "notificationType": "PAYMENT_CONFIRMED",
                                      "eventId": "payment-20260522-0001",
                                      "requestedChannel": "EMAIL",
                                      "status": "PENDING",
                                      "deliveries": [
                                        {
                                          "deliveryId": 1,
                                          "channel": "EMAIL",
                                          "status": "PENDING",
                                          "attemptCount": 0,
                                          "maxAttempts": 5,
                                          "availableAt": "2026-05-22T19:30:00",
                                          "lastFailureCode": null,
                                          "lastFailureMessage": null,
                                          "succeededAt": null,
                                          "failedAt": null
                                        }
                                      ],
                                      "duplicated": true,
                                      "createdAt": "2026-05-22T19:30:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 형식 또는 입력값 오류",
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
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "동일 이벤트 알림이 다른 채널로 이미 존재",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "NOTIFICATION_CHANNEL_CONFLICT",
                                        "status": "409",
                                        "message": "동일 알림 요청이 다른 채널로 이미 존재합니다."
                                      }
                                    }
                                    """)
                    )
            )
    })
    @PostMapping
    ResponseEntity<CreateNotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request
    );

    @Operation(
            summary = "알림 처리 상태 조회",
            description = """
                    내부 시스템 또는 운영자가 특정 알림 요청의 현재 처리 상태를 확인할 때 사용합니다.

                    * 이 API는 사용자용 알림함 조회가 아니라 운영자/내부 시스템용 상태 조회 API입니다.
                    * `notificationId`는 알림 요청 등록 응답으로 반환된 내부 알림 요청 ID입니다.
                    * 운영자는 `FAILED` 상태와 실패 사유를 확인해 수동 처리 여부를 판단할 수 있습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = NotificationDetailResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "notificationId": 1,
                                      "recipientId": "user-1001",
                                      "notificationType": "PAYMENT_CONFIRMED",
                                      "eventId": "payment-20260522-0001",
                                      "requestedChannel": "EMAIL",
                                      "referenceData": {
                                        "paymentId": "pay-9001",
                                        "courseId": "course-3001"
                                      },
                                      "status": "PENDING",
                                      "deliveries": [
                                        {
                                          "deliveryId": 1,
                                          "channel": "EMAIL",
                                          "status": "PENDING",
                                          "attemptCount": 0,
                                          "maxAttempts": 5,
                                          "availableAt": "2026-05-22T19:30:00",
                                          "lastFailureCode": null,
                                          "lastFailureMessage": null,
                                          "succeededAt": null,
                                          "failedAt": null
                                        }
                                      ],
                                      "createdAt": "2026-05-22T19:30:00",
                                      "updatedAt": "2026-05-22T19:30:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "알림 요청을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "NOTIFICATION_NOT_FOUND",
                                        "status": "404",
                                        "message": "알림 요청을 찾을 수 없습니다."
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/{notificationId}")
    ResponseEntity<NotificationDetailResponse> getNotificationDetail(
            @Parameter(description = "알림 요청 ID", example = "1")
            @PathVariable Long notificationId
    );
}
