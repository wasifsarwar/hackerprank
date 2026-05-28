package com.hackerprank.problems;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hackerprank.openai.OpenAiHttpResponse;
import com.hackerprank.openai.OpenAiTransport;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
class OpenAiProblemGenerator {
    private static final String SYSTEM_PROMPT = """
        You generate original HackerPrank coding practice problems for interview preparation.
        Return one self-contained stdin/stdout problem. The problem must be solvable in Python and Java.
        Do not copy known LeetCode, HackerRank, or web problems. Do not include Markdown code fences.
        Java code must use a public Main class. All outputs must be exact, deterministic, and newline-safe.
        """;

    private static final String PROBLEM_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "topic": { "type": "string" },
            "difficulty": { "type": "string", "enum": ["Easy", "Medium", "Hard"] },
            "problem": {
              "type": "object",
              "properties": {
                "id": { "type": "string" },
                "title": { "type": "string" },
                "difficulty": { "type": "string", "enum": ["Easy", "Medium", "Hard"] },
                "tags": { "type": "array", "items": { "type": "string" } },
                "description": { "type": "string" },
                "inputFormat": { "type": "string" },
                "outputFormat": { "type": "string" },
                "constraints": { "type": "array", "items": { "type": "string" } },
                "examples": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "input": { "type": "string" },
                      "output": { "type": "string" },
                      "explanation": { "type": "string" }
                    },
                    "required": ["input", "output", "explanation"],
                    "additionalProperties": false
                  }
                },
                "testCases": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "name": { "type": "string" },
                      "input": { "type": "string" },
                      "expectedOutput": { "type": "string" },
                      "hidden": { "type": "boolean" }
                    },
                    "required": ["name", "input", "expectedOutput", "hidden"],
                    "additionalProperties": false
                  }
                },
                "starterCode": {
                  "type": "object",
                  "properties": {
                    "python": { "type": "string" },
                    "java": { "type": "string" }
                  },
                  "required": ["python", "java"],
                  "additionalProperties": false
                }
              },
              "required": [
                "id",
                "title",
                "difficulty",
                "tags",
                "description",
                "inputFormat",
                "outputFormat",
                "constraints",
                "examples",
                "testCases",
                "starterCode"
              ],
              "additionalProperties": false
            },
            "referenceSolutions": {
              "type": "object",
              "properties": {
                "python": { "type": "string" },
                "java": { "type": "string" }
              },
              "required": ["python", "java"],
              "additionalProperties": false
            },
            "intendedTechnique": { "type": "string" }
          },
          "required": ["topic", "difficulty", "problem", "referenceSolutions", "intendedTechnique"],
          "additionalProperties": false
        }
        """;

    private final OpenAiProblemGeneratorProperties properties;
    private final ObjectMapper objectMapper;
    private final OpenAiTransport transport;

    OpenAiProblemGenerator(
        OpenAiProblemGeneratorProperties properties,
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

    GeneratedProblemSpec generate(String topic, String difficulty) {
        return generate(topic, difficulty, List.of(), "", "Classic");
    }

    GeneratedProblemSpec generate(
        String topic,
        String difficulty,
        List<String> targetConcepts,
        String constraintsNotes,
        String interviewStyle
    ) {
        return generate(topic, difficulty, targetConcepts, constraintsNotes, interviewStyle, "");
    }

    GeneratedProblemSpec repair(
        String topic,
        String difficulty,
        List<String> targetConcepts,
        String constraintsNotes,
        String interviewStyle,
        String validationError
    ) {
        return generate(topic, difficulty, targetConcepts, constraintsNotes, interviewStyle, validationError);
    }

    private GeneratedProblemSpec generate(
        String topic,
        String difficulty,
        List<String> targetConcepts,
        String constraintsNotes,
        String interviewStyle,
        String validationError
    ) {
        if (!isConfigured()) {
            throw new OpenAiProblemGenerationException("OpenAI generation requested without OPENAI_API_KEY");
        }

        String userPrompt = userPrompt(topic, difficulty, targetConcepts, constraintsNotes, interviewStyle, validationError);
        try {
            String requestBody = requestBody(userPrompt);
            OpenAiHttpResponse response = transport.post(
                properties.responsesUri(),
                properties.getApiKey(),
                requestBody,
                Duration.ofSeconds(properties.getTimeoutSeconds())
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new OpenAiProblemGenerationException("OpenAI returned HTTP " + response.statusCode());
            }

            String outputText = extractOutputText(objectMapper.readTree(response.body()));
            OpenAiProblemPayload payload = objectMapper.readValue(outputText, OpenAiProblemPayload.class);
            return toSpec(payload, topic, difficulty, targetConcepts, constraintsNotes, interviewStyle, userPrompt);
        } catch (OpenAiProblemGenerationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new OpenAiProblemGenerationException("OpenAI generation failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OpenAiProblemGenerationException("OpenAI generation was interrupted", exception);
        } catch (RuntimeException exception) {
            throw new OpenAiProblemGenerationException("OpenAI generation was not configured correctly", exception);
        }
    }

    String requestBodyForTest(String topic, String difficulty) {
        return requestBody(userPrompt(topic, difficulty, List.of(), "", "Classic", ""));
    }

    private String requestBody(String userPrompt) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", properties.getModel());
            request.put("instructions", SYSTEM_PROMPT);
            request.put("input", userPrompt);
            request.put("store", false);
            request.put("max_output_tokens", properties.getMaxOutputTokens());

            ObjectNode format = request.putObject("text").putObject("format");
            format.put("type", "json_schema");
            format.put("name", "hackerprank_generated_problem");
            format.put("description", "A generated HackerPrank coding problem draft.");
            format.set("schema", objectMapper.readTree(PROBLEM_SCHEMA));
            format.put("strict", true);

            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new OpenAiProblemGenerationException("Could not build OpenAI request", exception);
        }
    }

    private String userPrompt(
        String topic,
        String difficulty,
        List<String> targetConcepts,
        String constraintsNotes,
        String interviewStyle,
        String validationError
    ) {
        String repairInstructions = repairInstructions(validationError);
        return """
            Create one original interview-style coding problem.

            Requested topic: %s
            Requested difficulty: %s
            Target concepts: %s
            Constraints or notes: %s
            Interview style: %s

            Requirements:
            - Use simple stdin/stdout input and output only.
            - Include at least two examples.
            - Include at least two visible tests and at least three hidden tests.
            - Include Python starter code and Java starter code with TODO comments.
            - Include complete Python and Java reference solutions that pass every test case.
            - Keep the Java reference solution and starter code in a public class named Main.
            - Make the intended technique clear enough for a tutor to explain later.
            %s
            """.formatted(
                topic,
                difficulty,
                formatConcepts(targetConcepts),
                textOrDefault(constraintsNotes, "none"),
                textOrDefault(interviewStyle, "Classic"),
                repairInstructions
            );
    }

    private String extractOutputText(JsonNode response) {
        JsonNode error = response.get("error");
        if (error != null && !error.isNull()) {
            throw new OpenAiProblemGenerationException("OpenAI response error: " + error);
        }

        String status = response.path("status").asText("");
        if ("incomplete".equals(status)) {
            throw new OpenAiProblemGenerationException("OpenAI response was incomplete: " + response.path("incomplete_details"));
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
                    throw new OpenAiProblemGenerationException("OpenAI refused generation: " + contentItem.path("refusal").asText());
                }
                if ("output_text".equals(type)) {
                    outputText.append(contentItem.path("text").asText());
                }
            }
        }

        if (outputText.isEmpty()) {
            throw new OpenAiProblemGenerationException("OpenAI response did not contain output_text content");
        }

        return outputText.toString();
    }

    private GeneratedProblemSpec toSpec(
        OpenAiProblemPayload payload,
        String requestedTopic,
        String requestedDifficulty,
        List<String> targetConcepts,
        String constraintsNotes,
        String interviewStyle,
        String userPrompt
    ) throws JsonProcessingException {
        OpenAiProblem problemPayload = payload.problem();
        if (problemPayload == null) {
            throw new OpenAiProblemGenerationException("OpenAI response did not include a problem");
        }

        String topic = textOrDefault(payload.topic(), requestedTopic);
        String difficulty = textOrDefault(payload.difficulty(), requestedDifficulty);
        Problem problem = new Problem(
            textOrDefault(problemPayload.id(), problemPayload.title()),
            problemPayload.title(),
            textOrDefault(problemPayload.difficulty(), difficulty),
            listOrEmpty(problemPayload.tags()),
            problemPayload.description(),
            problemPayload.inputFormat(),
            problemPayload.outputFormat(),
            listOrEmpty(problemPayload.constraints()),
            examples(problemPayload.examples()),
            testCases(problemPayload.testCases()),
            languageMap(problemPayload.starterCode())
        );

        return new GeneratedProblemSpec(
            topic,
            difficulty,
            problem,
            languageMap(payload.referenceSolutions()),
            generationMetadata(
                topic,
                difficulty,
                targetConcepts,
                constraintsNotes,
                interviewStyle,
                userPrompt,
                payload.intendedTechnique()
            )
        );
    }

    private GenerationMetadata generationMetadata(
        String topic,
        String difficulty,
        List<String> targetConcepts,
        String constraintsNotes,
        String interviewStyle,
        String userPrompt,
        String intendedTechnique
    ) throws JsonProcessingException {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("topic", topic);
        parameters.put("difficulty", difficulty);
        parameters.put("targetConcepts", targetConcepts == null ? List.of() : targetConcepts);
        parameters.put("constraintsNotes", textOrDefault(constraintsNotes, ""));
        parameters.put("interviewStyle", textOrDefault(interviewStyle, "Classic"));
        parameters.put("model", properties.getModel());
        parameters.put("maxOutputTokens", properties.getMaxOutputTokens());
        parameters.put("responsesUrl", properties.getResponsesUrl());

        return new GenerationMetadata(
            "openai",
            properties.getModel(),
            properties.getPromptVersion(),
            SYSTEM_PROMPT + "\n\n" + userPrompt,
            objectMapper.writeValueAsString(parameters),
            "PENDING",
            "",
            "",
            textOrDefault(intendedTechnique, "")
        );
    }

    private List<Example> examples(List<OpenAiExample> examples) {
        return listOrEmpty(examples).stream()
            .map(example -> new Example(example.input(), example.output(), example.explanation()))
            .toList();
    }

    private List<TestCase> testCases(List<OpenAiTestCase> testCases) {
        return listOrEmpty(testCases).stream()
            .map(testCase -> new TestCase(
                testCase.name(),
                testCase.input(),
                testCase.expectedOutput(),
                testCase.hidden()
            ))
            .toList();
    }

    private Map<String, String> languageMap(Map<String, String> values) {
        if (values == null) {
            return Map.of();
        }

        Map<String, String> ordered = new LinkedHashMap<>();
        ordered.put("python", values.get("python"));
        ordered.put("java", values.get("java"));
        return ordered;
    }

    private String formatConcepts(List<String> targetConcepts) {
        if (targetConcepts == null || targetConcepts.isEmpty()) {
            return "none";
        }

        return String.join(", ", targetConcepts);
    }

    private String repairInstructions(String validationError) {
        if (validationError == null || validationError.isBlank()) {
            return "";
        }

        return """

            Repair context:
            - A previous draft failed validation with this error: %s
            - Return a complete replacement draft that fixes the validation failure.
            - Do not explain the repair. Return only the structured JSON required by the schema.
            """.formatted(validationError);
    }

    private <T> List<T> listOrEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String textOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null ? "" : fallback;
        }

        return value;
    }

    private record OpenAiProblemPayload(
        String topic,
        String difficulty,
        OpenAiProblem problem,
        Map<String, String> referenceSolutions,
        String intendedTechnique
    ) {
    }

    private record OpenAiProblem(
        String id,
        String title,
        String difficulty,
        List<String> tags,
        String description,
        String inputFormat,
        String outputFormat,
        List<String> constraints,
        List<OpenAiExample> examples,
        List<OpenAiTestCase> testCases,
        Map<String, String> starterCode
    ) {
    }

    private record OpenAiExample(String input, String output, String explanation) {
    }

    private record OpenAiTestCase(String name, String input, String expectedOutput, boolean hidden) {
    }
}
