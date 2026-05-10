package com.example.datn.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class GeminiConfig {

    @Bean(name = "geminiRestTemplate")
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(60000);
        return new RestTemplate(factory);
    }
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
