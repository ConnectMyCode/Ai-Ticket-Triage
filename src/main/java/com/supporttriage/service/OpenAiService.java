package com.supporttriage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * IMP note : Though the file name is OPENAI Service but it is using Groq Api services... 
 * Handles Groq API communication safely
 */

@Service
public class OpenAiService {

    @Value("${ai.provider.api.key}")
    private String apiKey;

    @Value("${ai.model}")
    private String model;

    private final RestTemplate restTemplate;

    public OpenAiService() {

        // Timeout protection
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(10000); // 10 sec
        factory.setReadTimeout(15000);    // 15 sec

        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Sends prompt to OpenAI and returns raw response
     */
    public String runTriage(String prompt) {

        try {

            String url = "https://api.groq.com/openai/v1/chat/completions";
            										
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", new Object[]{
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    },
                    "temperature", 0.0,
                    "max_completion_tokens", 1000,
                    "reasoning_effort", "low",
                    "include_reasoning", false
            );
            
            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getBody() == null || response.getBody().isEmpty()) {
                throw new RuntimeException("Malformed AI response");
            }

            return response.getBody();

        } catch (Exception e) {

            String message = e.getMessage();

            if (message != null && message.contains("401")) {
                throw new RuntimeException("Invalid AI API key");
            }

            if (message != null &&
                    (message.contains("timed out")
                            || message.contains("Read timed out"))) {
                throw new RuntimeException("AI request timeout");
            }

            throw new RuntimeException("AI provider failed");
        }
    }
}