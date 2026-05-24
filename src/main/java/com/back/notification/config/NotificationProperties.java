package com.back.notification.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private final Worker worker = new Worker();
    private final Retry retry = new Retry();

    public Worker getWorker() {
        return worker;
    }

    public Retry getRetry() {
        return retry;
    }

    public static class Worker {
        private boolean enabled = true;
        private String id = "local-worker";
        private int batchSize = 20;
        private Duration lockDuration = Duration.ofSeconds(30);
        private Duration dispatchFixedDelay = Duration.ofSeconds(2);
        private Duration recoveryFixedDelay = Duration.ofSeconds(30);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public Duration getLockDuration() {
            return lockDuration;
        }

        public void setLockDuration(Duration lockDuration) {
            this.lockDuration = lockDuration;
        }

        public Duration getDispatchFixedDelay() {
            return dispatchFixedDelay;
        }

        public void setDispatchFixedDelay(Duration dispatchFixedDelay) {
            this.dispatchFixedDelay = dispatchFixedDelay;
        }

        public Duration getRecoveryFixedDelay() {
            return recoveryFixedDelay;
        }

        public void setRecoveryFixedDelay(Duration recoveryFixedDelay) {
            this.recoveryFixedDelay = recoveryFixedDelay;
        }
    }

    public static class Retry {
        private int maxAttempts = 5;
        private Duration initialDelay = Duration.ofSeconds(2);
        private int multiplier = 2;
        private Duration maxDelay = Duration.ofSeconds(32);

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
        }

        public int getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(int multiplier) {
            this.multiplier = multiplier;
        }

        public Duration getMaxDelay() {
            return maxDelay;
        }

        public void setMaxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
        }
    }
}
