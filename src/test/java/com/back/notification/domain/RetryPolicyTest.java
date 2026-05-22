package com.back.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("재시도 정책")
class RetryPolicyTest {

    @Test
    @DisplayName("시도 횟수에 따라 지수 백오프 방식으로 다음 처리 가능 시각을 계산한다")
    void nextAvailableAtAppliesExponentialBackoff() {
        RetryPolicy retryPolicy = new RetryPolicy(
                5,
                Duration.ofSeconds(10),
                3,
                Duration.ofMinutes(15)
        );
        LocalDateTime now = LocalDateTime.of(2026, 5, 22, 10, 0);

        assertThat(retryPolicy.nextAvailableAt(1, now)).isEqualTo(now.plusSeconds(10));
        assertThat(retryPolicy.nextAvailableAt(2, now)).isEqualTo(now.plusSeconds(30));
        assertThat(retryPolicy.nextAvailableAt(3, now)).isEqualTo(now.plusSeconds(90));
    }
}
