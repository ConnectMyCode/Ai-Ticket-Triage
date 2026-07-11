package com.supporttriage.service;


/*
🚨 RESPONSIBILITY:
ONLY:
Parse JSON safely
Reject malformed JSON
Return AiTriageResponse
* */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supporttriage.dto.AiTriageResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses raw OpenAI response safely
 */
@Service
public class AiTriageParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse Groq AI JSON response
     */
    public AiTriageResponse parse(String rawResponse) {

        try {

            JsonNode root = objectMapper.readTree(rawResponse);

            String content = root
                    .get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();

            JsonNode aiJson = objectMapper.readTree(content);

            List<String> tags = new ArrayList<>();

            if (aiJson.has("tags")) {
                for (JsonNode tag : aiJson.get("tags")) {
                    tags.add(tag.asText());
                }
            }

            return new AiTriageResponse(
                    aiJson.get("category").asText(),
                    aiJson.get("priority").asText(),
                    tags,
                    aiJson.get("sentiment").asText(),
                    aiJson.get("summary").asText(),
                    aiJson.get("replyDraft").asText()
            );

        } catch (Exception e) {
            throw new RuntimeException("Invalid AI JSON response");
        }
    }
}







