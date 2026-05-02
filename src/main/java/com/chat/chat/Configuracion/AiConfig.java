package com.chat.chat.Configuracion;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Bean
    @Qualifier("deepSeekRestTemplate")
    public RestTemplate deepSeekRestTemplate(RestTemplateBuilder builder, DeepSeekProperties deepSeekProperties) {
        Duration timeout = Duration.ofSeconds(deepSeekProperties.getTimeoutSeconds());
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }

    @Bean
    @Qualifier("deepSeekAdminReportRestTemplate")
    public RestTemplate deepSeekAdminReportRestTemplate(RestTemplateBuilder builder, DeepSeekProperties deepSeekProperties) {
        Duration timeout = Duration.ofSeconds(deepSeekProperties.getAdminReportTimeoutSeconds());
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }
}
