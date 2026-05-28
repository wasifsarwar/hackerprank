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
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
class AnthropicTutorHintGenerator {
    private static final int MAX_HINT_LENGTH = 360;
    private static final int MAX_HINTS = 4;
    private static final Set<String> ALLOWED_LEVELS = Set.of("nudge", "diagnostic", "strategy");
    private static final String PROVIDER = "anthropic";

    private static final String SYSTEM_PROMPT = """
        You are the HackerPrank tutor for interview coding practice.
        Give one focused, progressive hint for the current submission.
        Do not provide a full solution, complete code, reference solution, or hidden-test details.
        Hidden test names, inputs, expected outputs, actual outputs, and stderr are unavailable by design.
        If only hidden tests failed, discuss likely edge-case categories without inventing hidden test data.
        Keep the user thinking; prefer questions, debugging anchors, and the next smallest useful move.
        Return only valid JSON matching the requested schema.
        """;

    private static final String HINT_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "level": {
              "type": "string",
              "enum": ["nudge", "diagnostic", "strategy"]
            },
            "summary": { "type": "string" },
            "hints": {
              "type": "array",
              "items": { "type": "string" },
              "minItems": 1,
              "maxItems": 4
            },
            "nextStep": { "type": "string" }
          },
          "required": ["level", "summary", "hints", "nextStep"],
          "additionalProperties": false
        }
        """;

    private final AnthropicTutorProperties properties;
    private final ObjectMapper objectMapper;
    private final AnthropicTransport transport;

    AnthropicTutorHintGenerator(
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

    TutorHintResponse createHint(TutorHintContext context) {
        if (!isConfigured()) {
            throw new AnthropicTutorException("Anthropic tutor requested without ANTHROPIC_API_KEY");
        }

        try {
            String requestBody = requestBody(context);
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
            AnthropicTutorHintPayload payload = objectMapper.readValue(stripJsonFences(outputText), AnthropicTutorHintPayload.class);
            return response(context.submission(), payload);
        } catch (AnthropicTutorException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new AnthropicTutorException("Anthropic tutor hint failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AnthropicTutorException("Anthropic tutor hint was interrupted", exception);
        } catch (RuntimeException exception) {
            throw new AnthropicTutorException("Anthropic tutor hint was not configured correctly", exception);
        }
    }

    String requestBodyForTest(TutorHintContext context) {
        return requestBody(context);
    }

    private String requestBody(TutorHintContext context) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", properties.getModel());
            request.put("max_tokens", properties.getMaxOutputTokens());
            request.put("system", SYSTEM_PROMPT);

            ArrayNode messages = request.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content", userPrompt(context) + """

                Return only JSON. Do not wrap the JSON in markdown.

                JSON schema:
                """ + HINT_SCHEMA);

            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new AnthropicTutorException("Could not build Anthropic tutor request", exception);
        }
    }

    private String userPrompt(TutorHintContext context) throws JsonProcessingException {
        return """
            Create one tutor hint for this coding-practice submission.
            Use only the safe context JSON below.
            Do not guess hidden test details.
            Do not include a complete solution.

            Safe context JSON:
            %s
            """.formatted(objectMapper.writeValueAsString(
                TutorContextPayloadBuilder.safeContext(objectMapper, context, properties.getPromptVersion())
            ));
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

    private TutorHintResponse response(SubmissionDetail submission, AnthropicTutorHintPayload payload) {
        String level = normalizeLevel(payload.level());
        String summary = compact(payload.summary(), MAX_HINT_LENGTH);
        List<String> hints = listOrEmpty(payload.hints()).stream()
            .map(hint -> compact(hint, MAX_HINT_LENGTH))
            .filter(hint -> !hint.isBlank())
            .limit(MAX_HINTS)
            .toList();
        String nextStep = compact(payload.nextStep(), MAX_HINT_LENGTH);

        if (summary.isBlank() || hints.isEmpty() || nextStep.isBlank()) {
            throw new AnthropicTutorException("Anthropic tutor response omitted required hint fields");
        }

        return new TutorHintResponse(
            submission.getId(),
            normalizeStatus(submission.getStatus()),
            PROVIDER,
            level,
            summary,
            hints,
            nextStep
        );
    }

    private String normalizeLevel(String level) {
        String normalized = level == null ? "" : level.toLowerCase(Locale.ROOT);
        return ALLOWED_LEVELS.contains(normalized) ? normalized : "nudge";
    }

    private String normalizeStatus(String status) {
        return status == null ? "WRONG_ANSWER" : status.toUpperCase(Locale.ROOT);
    }

    private String compact(String value, int limit) {
        if (value == null) {
            return "";
        }

        String compacted = value.replace("\r\n", "\n").trim();
        if (compacted.length() <= limit) {
            return compacted;
        }
        return compacted.substring(0, limit) + "...";
    }

    private <T> List<T> listOrEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record AnthropicTutorHintPayload(String level, String summary, List<String> hints, String nextStep) {
    }
}
