package com.back.notification.domain;

import java.time.Duration;
import java.time.LocalDateTime;

public class RetryPolicy {

    private final int maxAttempts;
    private final Duration initialDelay;
    private final int multiplier;
    private final Duration maxDelay;

    public RetryPolicy(
            int maxAttempts,
            Duration initialDelay,
            int multiplier,
            Duration maxDelay
    ) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be greater than 0");
        }
        if (multiplier < 1) {
            throw new IllegalArgumentException("multiplier must be greater than 0");
        }
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
        this.multiplier = multiplier;
        this.maxDelay = maxDelay;
    }

    public boolean canRetry(int attemptCount) {
        return attemptCount < maxAttempts;
    }

    public LocalDateTime nextAvailableAt(int attemptCount, LocalDateTime now) {
        long exponent = Math.max(0, attemptCount - 1);
        long delaySeconds = initialDelay.toSeconds();
        for (long i = 0; i < exponent; i++) {
            delaySeconds = Math.multiplyExact(delaySeconds, multiplier);
            if (delaySeconds >= maxDelay.toSeconds()) {
                delaySeconds = maxDelay.toSeconds();
                break;
            }
        }
        return now.plusSeconds(Math.min(delaySeconds, maxDelay.toSeconds()));
    }

    public int maxAttempts() {
        return maxAttempts;
    }
}
