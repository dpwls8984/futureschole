package com.back.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Dispatch API")
                        .version("v1")
                        .description("비동기 알림 발송 요청, 상태 조회, 인앱 알림 조회/읽음 처리를 위한 API 문서입니다."));
    }
}
