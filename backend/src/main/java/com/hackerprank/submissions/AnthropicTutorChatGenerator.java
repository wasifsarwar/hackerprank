package com.hackerprank.submissions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hackerprank.anthropic.AnthropicHttpResponse;
import com.hackerprank.anthropic.AnthropicTransport;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
class AnthropicTutorChatGenerator {
    private static final int MAX_MESSAGE_LENGTH = 1200;
    private static final int MAX_REPLY_LENGTH = 900;
    private static final int MAX_HISTORY_MESSAGES = 8;
    private static final String PROVIDER = "anthropic";

    private static final String SYSTEM_PROMPT = """
        You are the HackerPrank tutor for interview coding practice.
        Answer the user's follow-up about the current failed submission.
        Do not provide a full solution, complete code, reference solution, or hidden-test details.
        Hidden test names, inputs, expected outputs, actual outputs, and stderr are unavailable by design.
        If the user asks for the answer, give a smaller hint or ask a guiding question instead.
        Keep responses concise, practical, and focused on the next debugging move.
        Return only valid JSON matching the requested schema.
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

    private final AnthropicTutorProperties properties;
    private final ObjectMapper objectMapper;
    private final AnthropicTransport transport;

    AnthropicTutorChatGenerator(
        AnthropicTutorProperties properties,
        ObjectMapper objectMapper,
        AnthropicTransport transport
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
            throw new AnthropicTutorException("Anthropic tutor chat requested without ANTHROPIC_API_KEY");
        }

        try {
            String requestBody = requestBody(context, recentMessages);
            AnthropicHttpResponse response = transport.post(
                properties.messagesUri(),
                properties.getApiKey(),
                properties.getVersion(),
                requestBody,
                Duration.ofSeconds(properties.getTimeoutSeconds())
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AnthropicTutorException("Anthropic returned HTTP " + response.statusCode());
            }

            String outputText = extractOutputText(objectMapper.readTree(response.body()));
            AnthropicTutorChatPayload payload = objectMapper.readValue(stripJsonFences(outputText), AnthropicTutorChatPayload.class);
            String message = TutorContextPayloadBuilder.compact(payload.message(), MAX_REPLY_LENGTH);
            if (message.isBlank()) {
                throw new AnthropicTutorException("Anthropic tutor chat response omitted message");
            }
            return new TutorChatReply(PROVIDER, message);
        } catch (AnthropicTutorException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new AnthropicTutorException("Anthropic tutor chat failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AnthropicTutorException("Anthropic tutor chat was interrupted", exception);
        } catch (RuntimeException exception) {
            throw new AnthropicTutorException("Anthropic tutor chat was not configured correctly", exception);
        }
    }

    String requestBodyForTest(TutorHintContext context, List<TutorMessage> recentMessages) {
        return requestBody(context, recentMessages);
    }

    private String requestBody(TutorHintContext context, List<TutorMessage> recentMessages) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", properties.getModel());
            request.put("max_tokens", properties.getMaxOutputTokens());
            request.put("system", SYSTEM_PROMPT);

            ArrayNode messages = request.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content", userPrompt(context, recentMessages) + """

                Return only JSON. Do not wrap the JSON in markdown.

                JSON schema:
                """ + CHAT_SCHEMA);

            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new AnthropicTutorException("Could not build Anthropic tutor chat request", exception);
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
            throw new AnthropicTutorException("Anthropic response error: " + error);
        }

        String stopReason = response.path("stop_reason").asText("");
        if ("max_tokens".equals(stopReason)) {
            throw new AnthropicTutorException("Anthropic response reached max_tokens before completion");
        }

        StringBuilder outputText = new StringBuilder();
        for (JsonNode contentItem : response.path("content")) {
            if ("text".equals(contentItem.path("type").asText())) {
                outputText.append(contentItem.path("text").asText());
            }
        }

        if (outputText.isEmpty()) {
            throw new AnthropicTutorException("Anthropic response did not contain text content");
        }

        return outputText.toString();
    }

    private String stripJsonFences(String outputText) {
        String text = outputText == null ? "" : outputText.trim();
        if (!text.startsWith("```")) {
            return text;
        }

        int firstNewline = text.indexOf('\n');
        if (firstNewline >= 0) {
            text = text.substring(firstNewline + 1).trim();
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3).trim();
        }
        return text;
    }

    private record AnthropicTutorChatPayload(String message) {
    }
}
