package com.hackerprank.problems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hackerprank.anthropic.AnthropicHttpResponse;
import com.hackerprank.anthropic.AnthropicTransport;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class AnthropicProblemGeneratorTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void requestsMessagesApiJsonAndMapsResponseIntoGeneratedProblemSpec() throws Exception {
        AnthropicProblemGeneratorProperties properties = properties();
        StubTransport transport = new StubTransport(responseBody("```json\n" + problemJson() + "\n```"));
        AnthropicProblemGenerator generator = new AnthropicProblemGenerator(properties, objectMapper, transport);

        GeneratedProblemSpec spec = generator.generate(
            "graphs",
            "Medium",
            List.of("reachability", "edge cases"),
            "Avoid recursion-heavy solutions.",
            "Performance"
        );

        assertEquals("graphs", spec.topic());
        assertEquals("Medium", spec.difficulty());
        assertEquals("trail-checkpoints", spec.problem().getId());
        assertEquals("Trail Checkpoints", spec.problem().getTitle());
        assertEquals("anthropic", spec.generationMetadata().provider());
        assertEquals("claude-sonnet-4-6", spec.generationMetadata().modelId());
        assertEquals("anthropic-problem-v1", spec.generationMetadata().promptVersion());
        assertTrue(spec.generationMetadata().promptText().contains("Requested topic: graphs"));
        assertTrue(spec.generationMetadata().promptText().contains("Target concepts: reachability, edge cases"));
        assertTrue(spec.generationMetadata().promptText().contains("Starter code must include the complete stdin parsing"));
        assertTrue(spec.generationMetadata().promptText().contains("Starter code must call a named TODO helper/function"));
        assertFalse(spec.generationMetadata().parametersJson().contains("sk-ant-test"));
        assertTrue(spec.referenceSolutions().get("python").contains("print(total)"));
        assertTrue(spec.referenceSolutions().get("java").contains("class Main"));

        JsonNode parameters = objectMapper.readTree(spec.generationMetadata().parametersJson());
        assertEquals("Performance", parameters.path("interviewStyle").asText());
        assertEquals("reachability", parameters.path("targetConcepts").get(0).asText());
        assertEquals("Avoid recursion-heavy solutions.", parameters.path("constraintsNotes").asText());
        assertEquals("2023-06-01", parameters.path("anthropicVersion").asText());

        JsonNode request = objectMapper.readTree(transport.requestBody);
        assertEquals("claude-sonnet-4-6", request.path("model").asText());
        assertEquals(6000, request.path("max_tokens").asInt());
        assertTrue(request.path("system").asText().contains("HackerPrank coding practice problems"));
        assertEquals("user", request.path("messages").get(0).path("role").asText());
        String userContent = request.path("messages").get(0).path("content").asText();
        assertTrue(userContent.contains("Requested topic: graphs"));
        assertTrue(userContent.contains("Starter code must call a named TODO helper/function"));
        assertTrue(userContent.contains("JSON schema:"));
        assertTrue(userContent.contains("\"referenceSolutions\""));
        assertEquals("sk-ant-test", transport.apiKey);
        assertEquals("2023-06-01", transport.anthropicVersion);
        assertEquals(URI.create("https://api.anthropic.test/v1/messages"), transport.uri);
    }

    private AnthropicProblemGeneratorProperties properties() {
        AnthropicProblemGeneratorProperties properties = new AnthropicProblemGeneratorProperties();
        properties.setApiKey("sk-ant-test");
        properties.setModel("claude-sonnet-4-6");
        properties.setMessagesUrl("https://api.anthropic.test/v1/messages");
        properties.setVersion("2023-06-01");
        properties.setTimeoutSeconds(45);
        properties.setMaxOutputTokens(6000);
        properties.setPromptVersion("anthropic-problem-v1");
        return properties;
    }

    private String responseBody(String outputText) throws Exception {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "message");
        response.put("role", "assistant");
        response.put("model", "claude-sonnet-4-6");
        response.put("stop_reason", "end_turn");
        ArrayNode content = response.putArray("content");
        ObjectNode text = content.addObject();
        text.put("type", "text");
        text.put("text", outputText);
        return objectMapper.writeValueAsString(response);
    }

    private String problemJson() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("topic", "graphs");
        root.put("difficulty", "Medium");
        root.put("intendedTechnique", "Track reachable checkpoints with a single pass over directed edges.");

        ObjectNode problem = root.putObject("problem");
        problem.put("id", "trail-checkpoints");
        problem.put("title", "Trail Checkpoints");
        problem.put("difficulty", "Medium");
        problem.putArray("tags").add("Graphs").add("Reachability");
        problem.put("description", "Count checkpoints that can be reached from checkpoint one.");
        problem.put("inputFormat", "The first line contains n and m. The next m lines contain directed edges.");
        problem.put("outputFormat", "Print the number of reachable checkpoints.");
        problem.putArray("constraints").add("1 <= n <= 100000").add("0 <= m <= 200000");

        ArrayNode examples = problem.putArray("examples");
        examples.add(example("4 3\n1 2\n2 3\n4 1\n", "3\n", "Checkpoints 1, 2, and 3 are reachable."));
        examples.add(example("3 0\n", "1\n", "Only the starting checkpoint is reachable."));

        ArrayNode testCases = problem.putArray("testCases");
        testCases.add(testCase("Sample 1", "4 3\n1 2\n2 3\n4 1\n", "3\n", false));
        testCases.add(testCase("Sample 2", "3 0\n", "1\n", false));
        testCases.add(testCase("Hidden 1", "5 4\n1 2\n2 3\n3 4\n4 5\n", "5\n", true));
        testCases.add(testCase("Hidden 2", "4 2\n2 3\n3 4\n", "1\n", true));
        testCases.add(testCase("Hidden 3", "1 0\n", "1\n", true));

        ObjectNode starterCode = problem.putObject("starterCode");
        starterCode.put(
            "python",
            "import sys\n\ndef main():\n    print(count_reachable([]))\n\ndef count_reachable(edges):\n    # TODO: count reachable checkpoints\n    return 0\n\nif __name__ == \"__main__\":\n    main()\n"
        );
        starterCode.put(
            "java",
            "public class Main { public static void main(String[] args) { System.out.println(countReachable()); } static int countReachable() { return 0; } }\n"
        );

        ObjectNode referenceSolutions = root.putObject("referenceSolutions");
        referenceSolutions.put("python", "import sys\ntotal = 1\nprint(total)\n");
        referenceSolutions.put("java", "public class Main { public static void main(String[] args) { System.out.println(1); } }\n");

        return objectMapper.writeValueAsString(root);
    }

    private ObjectNode example(String input, String output, String explanation) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("input", input);
        node.put("output", output);
        node.put("explanation", explanation);
        return node;
    }

    private ObjectNode testCase(String name, String input, String expectedOutput, boolean hidden) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", name);
        node.put("input", input);
        node.put("expectedOutput", expectedOutput);
        node.put("hidden", hidden);
        return node;
    }

    private static class StubTransport implements AnthropicTransport {
        private final String responseBody;
        private URI uri;
        private String apiKey;
        private String anthropicVersion;
        private String requestBody;

        StubTransport(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public AnthropicHttpResponse post(
            URI uri,
            String apiKey,
            String anthropicVersion,
            String requestBody,
            Duration timeout
        ) {
            this.uri = uri;
            this.apiKey = apiKey;
            this.anthropicVersion = anthropicVersion;
            this.requestBody = requestBody;
            return new AnthropicHttpResponse(200, responseBody);
        }
    }
}
