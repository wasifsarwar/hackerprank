package com.hackerprank.submissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hackerprank.openai.OpenAiHttpResponse;
import com.hackerprank.openai.OpenAiTransport;
import com.hackerprank.problems.Example;
import com.hackerprank.problems.PublicProblem;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class OpenAiTutorChatGeneratorTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void requestsStructuredChatWithOnlySafeSubmissionContext() throws Exception {
        OpenAiTutorProperties properties = properties();
        StubTransport transport = new StubTransport(responseBody(chatJson()));
        OpenAiTutorChatGenerator generator = new OpenAiTutorChatGenerator(properties, objectMapper, transport);

        TutorChatReply reply = generator.createReply(
            TutorHintContext.from(submission(), problem()),
            List.of(new TutorMessage("message-1", "submission-1", "user", "user", "Can you explain the mismatch?", Instant.EPOCH))
        );

        assertEquals("openai", reply.provider());
        assertTrue(reply.content().contains("visible sample"));

        JsonNode request = objectMapper.readTree(transport.requestBody);
        assertEquals("gpt-5-mini", request.path("model").asText());
        assertEquals("hackerprank_tutor_chat", request.path("text").path("format").path("name").asText());
        assertTrue(request.path("text").path("format").path("strict").asBoolean());

        String prompt = request.path("input").asText();
        assertTrue(prompt.contains("Can you explain the mismatch?"));
        assertTrue(prompt.contains("Visible Sum"));
        assertTrue(prompt.contains("Sample 1"));
        assertTrue(prompt.contains("failedCount\":1"));
        assertFalse(prompt.contains("Hidden Secret Case"));
        assertFalse(prompt.contains("999"));
        assertFalse(prompt.contains("123"));
        assertFalse(prompt.contains("secret stderr"));
    }

    @Test
    void requestBodyForTestUsesSameSafeContextPolicy() throws Exception {
        OpenAiTutorChatGenerator generator = new OpenAiTutorChatGenerator(
            properties(),
            objectMapper,
            new StubTransport(responseBody(chatJson()))
        );

        String requestBody = generator.requestBodyForTest(
            TutorHintContext.from(submission(), problem()),
            List.of(new TutorMessage("message-1", "submission-1", "user", "user", "What edge should I inspect?", Instant.EPOCH))
        );

        assertTrue(requestBody.contains("What edge should I inspect?"));
        assertFalse(requestBody.contains("Hidden Secret Case"));
        assertFalse(requestBody.contains("secret stderr"));
    }

    private OpenAiTutorProperties properties() {
        OpenAiTutorProperties properties = new OpenAiTutorProperties();
        properties.setApiKey("sk-test");
        properties.setModel("gpt-5-mini");
        properties.setResponsesUrl("https://api.openai.test/v1/responses");
        properties.setTimeoutSeconds(30);
        properties.setMaxOutputTokens(900);
        properties.setPromptVersion("openai-tutor-v1");
        return properties;
    }

    private SubmissionDetail submission() {
        return new SubmissionDetail(
            "submission-1",
            "visible-sum",
            "Visible Sum",
            "Easy",
            "python",
            "print(0)\n",
            true,
            "WRONG_ANSWER",
            1,
            3,
            "",
            Instant.EPOCH,
            List.of(
                new TestCaseResult("Sample 1", false, false, "6\n", "0\n", "", false, 0, 15),
                new TestCaseResult("Sample 2", false, true, "2\n", "2\n", "", false, 0, 14),
                new TestCaseResult("Hidden Secret Case", true, false, "999\n", "123\n", "secret stderr", false, 0, 19)
            )
        );
    }

    private PublicProblem problem() {
        return new PublicProblem(
            "visible-sum",
            "Visible Sum",
            "Easy",
            List.of("Arrays", "Implementation"),
            "Read n values and print their sum.",
            "The first line contains n. The second line contains n values.",
            "Print the sum.",
            List.of("1 <= n <= 1000"),
            List.of(new Example("3\n1 2 3\n", "6\n", "The values sum to 6.")),
            Map.of("python", "import sys\n# TODO\n", "java", "public class Main {}\n")
        );
    }

    private String responseBody(String outputText) throws Exception {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "completed");
        ArrayNode output = response.putArray("output");
        ObjectNode message = output.addObject();
        message.put("type", "message");
        ArrayNode content = message.putArray("content");
        ObjectNode text = content.addObject();
        text.put("type", "output_text");
        text.put("text", outputText);
        return objectMapper.writeValueAsString(response);
    }

    private String chatJson() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("message", "Trace the visible sample and compare when the accumulator first changes.");
        return objectMapper.writeValueAsString(root);
    }

    private static class StubTransport implements OpenAiTransport {
        private final String responseBody;
        private URI uri;
        private String apiKey;
        private String requestBody;

        StubTransport(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public OpenAiHttpResponse post(URI uri, String apiKey, String requestBody, Duration timeout) {
            this.uri = uri;
            this.apiKey = apiKey;
            this.requestBody = requestBody;
            return new OpenAiHttpResponse(200, responseBody);
        }
    }
}
