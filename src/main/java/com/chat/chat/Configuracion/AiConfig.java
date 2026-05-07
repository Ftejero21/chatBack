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

    @Bean
    @Qualifier("aiMessageSearchRestTemplate")
    public RestTemplate aiMessageSearchRestTemplate(RestTemplateBuilder builder,
                                                    TejechatAiServiceProperties tejechatAiServiceProperties) {
        Duration timeout = Duration.ofSeconds(tejechatAiServiceProperties.getTimeoutSeconds());
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }

    @Bean
    @Qualifier("aiEncryptedSummaryRestTemplate")
    public RestTemplate aiEncryptedSummaryRestTemplate(RestTemplateBuilder builder,
                                                       TejechatAiServiceProperties tejechatAiServiceProperties) {
        Duration timeout = Duration.ofSeconds(tejechatAiServiceProperties.getTimeoutSeconds());
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }

    @Bean
    @Qualifier("aiTextRestTemplate")
    public RestTemplate aiTextRestTemplate(RestTemplateBuilder builder,
                                           TejechatAiServiceProperties tejechatAiServiceProperties) {
        Duration timeout = Duration.ofSeconds(tejechatAiServiceProperties.getTimeoutSeconds());
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }

    @Bean
    @Qualifier("aiReportAnalysisRestTemplate")
    public RestTemplate aiReportAnalysisRestTemplate(RestTemplateBuilder builder,
                                                     TejechatAiServiceProperties tejechatAiServiceProperties) {
        Duration timeout = Duration.ofSeconds(tejechatAiServiceProperties.getTimeoutSeconds());
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }

    @Bean
    @Qualifier("aiQuickReplyRestTemplate")
    public RestTemplate aiQuickReplyRestTemplate(RestTemplateBuilder builder,
                                                 TejechatAiServiceProperties tejechatAiServiceProperties) {
        Duration timeout = Duration.ofSeconds(tejechatAiServiceProperties.getTimeoutSeconds());
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }

}
