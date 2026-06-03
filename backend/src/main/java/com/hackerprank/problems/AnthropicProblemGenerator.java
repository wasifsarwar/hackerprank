package com.hackerprank.problems;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hackerprank.anthropic.AnthropicHttpResponse;
import com.hackerprank.anthropic.AnthropicTransport;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
class AnthropicProblemGenerator {
    private static final String SYSTEM_PROMPT = """
        You generate original HackerPrank coding practice problems for interview preparation.
        Return one self-contained stdin/stdout problem. The problem must be solvable in Python and Java.
        Do not copy known LeetCode, HackerRank, or web problems. Do not include Markdown code fences.
        Java code must use a public Main class. All outputs must be exact, deterministic, and newline-safe.
        Return only valid JSON matching the requested schema.
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
                "scenario": { "type": "string" },
                "task": { "type": "string" },
                "javaSignature": { "type": "string" },
                "pythonSignature": { "type": "string" },
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
                "scenario",
                "task",
                "javaSignature",
                "pythonSignature",
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

    private final AnthropicProblemGeneratorProperties properties;
    private final ObjectMapper objectMapper;
    private final AnthropicTransport transport;

    AnthropicProblemGenerator(
        AnthropicProblemGeneratorProperties properties,
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
            throw new AnthropicProblemGenerationException("Anthropic generation requested without ANTHROPIC_API_KEY");
        }

        String userPrompt = userPrompt(topic, difficulty, targetConcepts, constraintsNotes, interviewStyle, validationError);
        try {
            String requestBody = requestBody(userPrompt);
            AnthropicHttpResponse response = transport.post(
                properties.messagesUri(),
                properties.getApiKey(),
                properties.getVersion(),
                requestBody,
                Duration.ofSeconds(properties.getTimeoutSeconds())
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AnthropicProblemGenerationException("Anthropic returned HTTP " + response.statusCode());
            }

            String outputText = extractOutputText(objectMapper.readTree(response.body()));
            AnthropicProblemPayload payload = objectMapper.readValue(stripJsonFences(outputText), AnthropicProblemPayload.class);
            return toSpec(payload, topic, difficulty, targetConcepts, constraintsNotes, interviewStyle, userPrompt);
        } catch (AnthropicProblemGenerationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new AnthropicProblemGenerationException("Anthropic generation failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AnthropicProblemGenerationException("Anthropic generation was interrupted", exception);
        } catch (RuntimeException exception) {
            throw new AnthropicProblemGenerationException("Anthropic generation was not configured correctly", exception);
        }
    }

    String requestBodyForTest(String topic, String difficulty) {
        return requestBody(userPrompt(topic, difficulty, List.of(), "", "Classic", ""));
    }

    private String requestBody(String userPrompt) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", properties.getModel());
            request.put("max_tokens", properties.getMaxOutputTokens());
            request.put("system", SYSTEM_PROMPT);

            ArrayNode messages = request.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content", userPrompt + """

                Return only JSON. Do not wrap the JSON in markdown.

                JSON schema:
                """ + PROBLEM_SCHEMA);

            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new AnthropicProblemGenerationException("Could not build Anthropic request", exception);
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
            - Create a concrete real-world scenario inspired by the requested topic/company context, not a generic textbook wrapper.
            - Include a separate scenario paragraph and task paragraph. The scenario should be 2-4 sentences; the task should precisely define what to compute.
            - Include a Java method signature and a Python function signature for the candidate's core solution.
            - The starter code must parse stdin in main, call that exact method/function signature, and print the returned value.
            - Include at least two examples with explanations that teach the rule and boundary behavior.
            - Include at least two visible tests and at least three hidden tests.
            - Include Python starter code and Java starter code with TODO comments.
            - Starter code must include the complete stdin parsing and output loop for the stated input format.
            - Starter code must call a named TODO helper/function where the candidate implements the intended algorithm.
            - Do not leave main as only Scanner setup or generic TODO comments; main should pass parsed values into the helper and print its return value.
            - Include complete Python and Java reference solutions that pass every test case.
            - Keep the Java reference solution and starter code in a public class named Main.
            - Make the statement detailed enough that a candidate can solve it without guessing hidden assumptions.
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
            throw new AnthropicProblemGenerationException("Anthropic response error: " + error);
        }

        String stopReason = response.path("stop_reason").asText("");
        if ("max_tokens".equals(stopReason)) {
            throw new AnthropicProblemGenerationException("Anthropic response reached max_tokens before completion");
        }

        StringBuilder outputText = new StringBuilder();
        for (JsonNode contentItem : response.path("content")) {
            if ("text".equals(contentItem.path("type").asText())) {
                outputText.append(contentItem.path("text").asText());
            }
        }

        if (outputText.isEmpty()) {
            throw new AnthropicProblemGenerationException("Anthropic response did not contain text content");
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

    private GeneratedProblemSpec toSpec(
        AnthropicProblemPayload payload,
        String requestedTopic,
        String requestedDifficulty,
        List<String> targetConcepts,
        String constraintsNotes,
        String interviewStyle,
        String userPrompt
    ) throws JsonProcessingException {
        AnthropicProblem problemPayload = payload.problem();
        if (problemPayload == null) {
            throw new AnthropicProblemGenerationException("Anthropic response did not include a problem");
        }

        String topic = textOrDefault(payload.topic(), requestedTopic);
        String difficulty = textOrDefault(payload.difficulty(), requestedDifficulty);
        Problem problem = new Problem(
            textOrDefault(problemPayload.id(), problemPayload.title()),
            problemPayload.title(),
            textOrDefault(problemPayload.difficulty(), difficulty),
            listOrEmpty(problemPayload.tags()),
            problemPayload.scenario(),
            problemPayload.task(),
            problemPayload.javaSignature(),
            problemPayload.pythonSignature(),
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
        parameters.put("messagesUrl", properties.getMessagesUrl());
        parameters.put("anthropicVersion", properties.getVersion());
        parameters.put("repairUsed", userPrompt.contains("Repair context:"));

        return new GenerationMetadata(
            "anthropic",
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

    private List<Example> examples(List<AnthropicExample> examples) {
        return listOrEmpty(examples).stream()
            .map(example -> new Example(example.input(), example.output(), example.explanation()))
            .toList();
    }

    private List<TestCase> testCases(List<AnthropicTestCase> testCases) {
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

    private record AnthropicProblemPayload(
        String topic,
        String difficulty,
        AnthropicProblem problem,
        Map<String, String> referenceSolutions,
        String intendedTechnique
    ) {
    }

    private record AnthropicProblem(
        String id,
        String title,
        String difficulty,
        List<String> tags,
        String scenario,
        String task,
        String javaSignature,
        String pythonSignature,
        String description,
        String inputFormat,
        String outputFormat,
        List<AnthropicExample> examples,
        List<AnthropicTestCase> testCases,
        List<String> constraints,
        Map<String, String> starterCode
    ) {
    }

    private record AnthropicExample(String input, String output, String explanation) {
    }

    private record AnthropicTestCase(String name, String input, String expectedOutput, boolean hidden) {
    }
}
