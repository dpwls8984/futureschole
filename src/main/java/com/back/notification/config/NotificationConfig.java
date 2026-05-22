package com.back.notification.config;

import com.back.notification.domain.RetryPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfig {

    @Bean
    public RetryPolicy retryPolicy(NotificationProperties properties) {
        NotificationProperties.Retry retry = properties.getRetry();
        return new RetryPolicy(
                retry.getMaxAttempts(),
                retry.getInitialDelay(),
                retry.getMultiplier(),
                retry.getMaxDelay()
        );
    }
}
