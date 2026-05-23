package com.back.notification.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "현재 화면에 렌더링된 인앱 알림 읽음 처리 요청")
public record MarkNotificationsReadRequest(
        @Schema(
                description = "현재 화면에 렌더링된 인앱 알림 ID 목록",
                example = "[101, 100, 99, 98, 97, 96, 95, 94, 93, 92]"
        )
        @NotEmpty
        @Size(max = 50)
        List<@NotNull @Positive Long> inboxIds
) {
}
