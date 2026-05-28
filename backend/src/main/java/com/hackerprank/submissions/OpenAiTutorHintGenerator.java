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
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
class OpenAiTutorHintGenerator {
    private static final int MAX_HINT_LENGTH = 360;
    private static final int MAX_HINTS = 4;
    private static final Set<String> ALLOWED_LEVELS = Set.of("nudge", "diagnostic", "strategy");
    private static final String PROVIDER = "openai";

    private static final String SYSTEM_PROMPT = """
        You are the HackerPrank tutor for interview coding practice.
        Give one focused, progressive hint for the current submission.
        Do not provide a full solution, complete code, reference solution, or hidden-test details.
        Hidden test names, inputs, expected outputs, actual outputs, and stderr are unavailable by design.
        If only hidden tests failed, discuss likely edge-case categories without inventing hidden test data.
        Keep the user thinking; prefer questions, debugging anchors, and the next smallest useful move.
        Return only the structured JSON required by the schema.
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

    private final OpenAiTutorProperties properties;
    private final ObjectMapper objectMapper;
    private final OpenAiTransport transport;

    OpenAiTutorHintGenerator(
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

    TutorHintResponse createHint(TutorHintContext context) {
        if (!isConfigured()) {
            throw new OpenAiTutorHintException("OpenAI tutor requested without OPENAI_API_KEY");
        }

        try {
            String requestBody = requestBody(context);
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
            OpenAiTutorHintPayload payload = objectMapper.readValue(outputText, OpenAiTutorHintPayload.class);
            return response(context.submission(), payload);
        } catch (OpenAiTutorHintException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new OpenAiTutorHintException("OpenAI tutor hint failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OpenAiTutorHintException("OpenAI tutor hint was interrupted", exception);
        } catch (RuntimeException exception) {
            throw new OpenAiTutorHintException("OpenAI tutor hint was not configured correctly", exception);
        }
    }

    String requestBodyForTest(TutorHintContext context) {
        return requestBody(context);
    }

    private String requestBody(TutorHintContext context) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", properties.getModel());
            request.put("instructions", SYSTEM_PROMPT);
            request.put("input", userPrompt(context));
            request.put("store", false);
            request.put("max_output_tokens", properties.getMaxOutputTokens());

            ObjectNode format = request.putObject("text").putObject("format");
            format.put("type", "json_schema");
            format.put("name", "hackerprank_tutor_hint");
            format.put("description", "A progressive HackerPrank tutor hint.");
            format.set("schema", objectMapper.readTree(HINT_SCHEMA));
            format.put("strict", true);

            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new OpenAiTutorHintException("Could not build OpenAI tutor request", exception);
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
                    throw new OpenAiTutorHintException("OpenAI refused tutor hint: " + contentItem.path("refusal").asText());
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

    private TutorHintResponse response(SubmissionDetail submission, OpenAiTutorHintPayload payload) {
        String level = normalizeLevel(payload.level());
        String summary = compact(payload.summary(), MAX_HINT_LENGTH);
        List<String> hints = listOrEmpty(payload.hints()).stream()
            .map(hint -> compact(hint, MAX_HINT_LENGTH))
            .filter(hint -> !hint.isBlank())
            .limit(MAX_HINTS)
            .toList();
        String nextStep = compact(payload.nextStep(), MAX_HINT_LENGTH);

        if (summary.isBlank() || hints.isEmpty() || nextStep.isBlank()) {
            throw new OpenAiTutorHintException("OpenAI tutor response omitted required hint fields");
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

    private String textOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        return value;
    }

    private <T> List<T> listOrEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record OpenAiTutorHintPayload(String level, String summary, List<String> hints, String nextStep) {
    }
}
