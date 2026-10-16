package com.voyanta.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class that exposes a shared WebClient.Builder bean.
 * Outgoing HTTP clients, such as OpenAiPlanGenerator calling the AI provider,
 * inject this builder to create their own WebClient.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}