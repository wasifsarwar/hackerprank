package com.hackerprank.submissions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hackerprank.openai.OpenAiHttpResponse;
import com.hackerprank.openai.OpenAiTransport;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
class OpenAiTutorChatGenerator {
    private static final int MAX_MESSAGE_LENGTH = 1200;
    private static final int MAX_REPLY_LENGTH = 900;
    private static final int MAX_HISTORY_MESSAGES = 8;
    private static final String PROVIDER = "openai";

    private static final String SYSTEM_PROMPT = """
        You are the HackerPrank tutor for interview coding practice.
        Answer the user's follow-up about the current failed submission.
        Do not provide a full solution, complete code, reference solution, or hidden-test details.
        Hidden test names, inputs, expected outputs, actual outputs, and stderr are unavailable by design.
        If the user asks for the answer, give a smaller hint or ask a guiding question instead.
        Keep responses concise, practical, and focused on the next debugging move.
        Return only the structured JSON required by the schema.
        """;

    private static final String CHAT_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "message": { "type": "string" }
          },
          "required": ["message"],
          "additionalProperties": false
        }
        """;

    private final OpenAiTutorProperties properties;
    private final ObjectMapper objectMapper;
    private final OpenAiTransport transport;

    OpenAiTutorChatGenerator(
        OpenAiTutorProperties properties,
        ObjectMapper objectMapper,
        OpenAiTransport transport
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.transport = transport;
    }

    boolean isConfigured() {
        return properties.hasApiKey();
    }

    TutorChatReply createReply(TutorHintContext context, List<TutorMessage> recentMessages) {
        if (!isConfigured()) {
            throw new OpenAiTutorHintException("OpenAI tutor chat requested without OPENAI_API_KEY");
        }

        try {
            String requestBody = requestBody(context, recentMessages);
            OpenAiHttpResponse response = transport.post(
                properties.responsesUri(),
                properties.getApiKey(),
                requestBody,
                Duration.ofSeconds(properties.getTimeoutSeconds())
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new OpenAiTutorHintException("OpenAI returned HTTP " + response.statusCode());
            }

            String outputText = extractOutputText(objectMapper.readTree(response.body()));
            OpenAiTutorChatPayload payload = objectMapper.readValue(outputText, OpenAiTutorChatPayload.class);
            String message = TutorContextPayloadBuilder.compact(payload.message(), MAX_REPLY_LENGTH);
            if (message.isBlank()) {
                throw new OpenAiTutorHintException("OpenAI tutor chat response omitted message");
            }
            return new TutorChatReply(PROVIDER, message);
        } catch (OpenAiTutorHintException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new OpenAiTutorHintException("OpenAI tutor chat failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OpenAiTutorHintException("OpenAI tutor chat was interrupted", exception);
        } catch (RuntimeException exception) {
            throw new OpenAiTutorHintException("OpenAI tutor chat was not configured correctly", exception);
        }
    }

    String requestBodyForTest(TutorHintContext context, List<TutorMessage> recentMessages) {
        return requestBody(context, recentMessages);
    }

    private String requestBody(TutorHintContext context, List<TutorMessage> recentMessages) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", properties.getModel());
            request.put("instructions", SYSTEM_PROMPT);
            request.put("input", userPrompt(context, recentMessages));
            request.put("store", false);
            request.put("max_output_tokens", properties.getMaxOutputTokens());

            ObjectNode format = request.putObject("text").putObject("format");
            format.put("type", "json_schema");
            format.put("name", "hackerprank_tutor_chat");
            format.put("description", "A concise HackerPrank tutor follow-up response.");
            format.set("schema", objectMapper.readTree(CHAT_SCHEMA));
            format.put("strict", true);

            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new OpenAiTutorHintException("Could not build OpenAI tutor chat request", exception);
        }
    }

    private String userPrompt(TutorHintContext context, List<TutorMessage> recentMessages) throws JsonProcessingException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set(
            "safeContext",
            TutorContextPayloadBuilder.safeContext(objectMapper, context, properties.getPromptVersion())
        );

        ArrayNode messages = payload.putArray("recentMessages");
        int startIndex = Math.max(0, recentMessages.size() - MAX_HISTORY_MESSAGES);
        for (TutorMessage message : recentMessages.subList(startIndex, recentMessages.size())) {
            ObjectNode node = messages.addObject();
            node.put("role", message.getRole());
            node.put("content", TutorContextPayloadBuilder.compact(message.getContent(), MAX_MESSAGE_LENGTH));
        }

        return """
            Continue this submission-scoped tutor conversation.
            Use only the safe context and recent messages in the JSON below.
            Do not guess hidden test details.
            Do not include a complete solution.

            Tutor payload JSON:
            %s
            """.formatted(objectMapper.writeValueAsString(payload));
    }

    private String extractOutputText(JsonNode response) {
        JsonNode error = response.get("error");
        if (error != null && !error.isNull()) {
            throw new OpenAiTutorHintException("OpenAI response error: " + error);
        }

        String status = response.path("status").asText("");
        if ("incomplete".equals(status)) {
            throw new OpenAiTutorHintException("OpenAI response was incomplete: " + response.path("incomplete_details"));
        }

        JsonNode sdkOutputText = response.get("output_text");
        if (sdkOutputText != null && sdkOutputText.isTextual()) {
            return sdkOutputText.asText();
        }

        StringBuilder outputText = new StringBuilder();
        for (JsonNode outputItem : response.path("output")) {
            for (JsonNode contentItem : outputItem.path("content")) {
                String type = contentItem.path("type").asText();
                if ("refusal".equals(type)) {
                    throw new OpenAiTutorHintException("OpenAI refused tutor chat: " + contentItem.path("refusal").asText());
                }
                if ("output_text".equals(type)) {
                    outputText.append(contentItem.path("text").asText());
                }
            }
        }

        if (outputText.isEmpty()) {
            throw new OpenAiTutorHintException("OpenAI response did not contain output_text content");
        }

        return outputText.toString();
    }

    private record OpenAiTutorChatPayload(String message) {
    }
}
