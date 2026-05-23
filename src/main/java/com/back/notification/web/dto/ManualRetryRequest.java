package com.back.notification.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "운영자 수동 재시도 요청")
public record ManualRetryRequest(
        @Schema(
                description = "수동 재시도 사유",
                example = "외부 이메일 서버 장애 복구 후 재시도합니다."
        )
        @Size(max = 1000)
        String reason
) {
}
