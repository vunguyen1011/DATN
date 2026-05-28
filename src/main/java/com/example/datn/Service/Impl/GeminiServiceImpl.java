package com.example.datn.Service.Impl;

import com.example.datn.Service.Interface.IGeminiService;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiServiceImpl implements IGeminiService {

    @Qualifier("geminiRestTemplate")
    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String geminiApiUrl;

    @Override
    @Retryable(
        retryFor = {HttpServerErrorException.class, ResourceAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    @SentinelResource(
        value = "gemini_api", 
        fallback = "handleGeminiFallback", 
        blockHandler = "handleGeminiBlock"
    )
    public String callGeminiApi(String prompt) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            throw new RuntimeException("API Key missing");
        }
        
        String url = geminiApiUrl + "?key=" + geminiApiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> partsMap = new HashMap<>();

        partsMap.put("parts", Collections.singletonList(Collections.singletonMap("text", prompt)));
        requestBody.put("contents", Collections.singletonList(partsMap));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            try {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                if (rootNode.has("error")) {
                    throw new RuntimeException("Gemini API error in response payload");
                }
                
                JsonNode candidates = rootNode.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        return parts.get(0).path("text").asText();
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Gemini response parse error", e);
            }
        }
        throw new RuntimeException("AI did not return a valid result.");
    }

    // fallback when exception is thrown
    public String handleGeminiFallback(String prompt, Throwable ex) {
        log.error("Gemini API error (fallback triggered): {}", ex.getMessage());
        throw new RuntimeException("Gemini API connection/execution failed: " + ex.getMessage(), ex);
    }

    // blockHandler when circuit is open or rate limited
    public String handleGeminiBlock(String prompt, BlockException ex) {
        log.warn("Gemini API blocked by Sentinel (circuit open or rate limited). Reason: {}", ex.getClass().getSimpleName());
        throw new RuntimeException("Gemini API is temporarily unavailable due to Sentinel protection.", ex);
    }
}
