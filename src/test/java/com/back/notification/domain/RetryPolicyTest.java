package com.back.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    @Test
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
