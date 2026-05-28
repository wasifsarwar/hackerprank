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

class OpenAiTutorHintGeneratorTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void requestsStructuredHintWithOnlySafeSubmissionContext() throws Exception {
        OpenAiTutorProperties properties = properties();
        StubTransport transport = new StubTransport(responseBody(hintJson()));
        OpenAiTutorHintGenerator generator = new OpenAiTutorHintGenerator(properties, objectMapper, transport);
        TutorHintContext context = TutorHintContext.from(submission(), problem());

        TutorHintResponse hint = generator.createHint(context);

        assertEquals("openai", hint.getProvider());
        assertEquals("diagnostic", hint.getLevel());
        assertEquals("submission-1", hint.getSubmissionId());
        assertTrue(hint.getSummary().contains("visible mismatch"));
        assertTrue(hint.getHints().getFirst().contains("trace the accumulator"));
        assertEquals("sk-test", transport.apiKey);
        assertEquals(URI.create("https://api.openai.test/v1/responses"), transport.uri);

        JsonNode request = objectMapper.readTree(transport.requestBody);
        assertEquals("gpt-5-mini", request.path("model").asText());
        assertFalse(request.path("store").asBoolean());
        assertEquals(900, request.path("max_output_tokens").asInt());
        assertEquals("json_schema", request.path("text").path("format").path("type").asText());
        assertEquals("hackerprank_tutor_hint", request.path("text").path("format").path("name").asText());
        assertTrue(request.path("text").path("format").path("strict").asBoolean());

        String prompt = request.path("input").asText();
        assertTrue(prompt.contains("Visible Sum"));
        assertTrue(prompt.contains("print(0)"));
        assertTrue(prompt.contains("Sample 1"));
        assertTrue(prompt.contains("expectedOutput\":\"6"));
        assertTrue(prompt.contains("actualOutput\":\"0"));
        assertTrue(prompt.contains("failedCount\":1"));
        assertTrue(prompt.contains("Hidden test names, inputs, expected outputs, actual outputs, and stderr are not available."));

        assertFalse(prompt.contains("Hidden Secret Case"));
        assertFalse(prompt.contains("999"));
        assertFalse(prompt.contains("123"));
        assertFalse(prompt.contains("secret stderr"));
    }

    @Test
    void requestBodyForTestUsesSameSafeContextPolicy() throws Exception {
        OpenAiTutorHintGenerator generator = new OpenAiTutorHintGenerator(
            properties(),
            objectMapper,
            new StubTransport(responseBody(hintJson()))
        );

        String requestBody = generator.requestBodyForTest(TutorHintContext.from(submission(), problem()));

        assertTrue(requestBody.contains("Sample 1"));
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

    private String hintJson() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("level", "diagnostic");
        root.put("summary", "The visible mismatch points at the value you are accumulating.");
        root.putArray("hints")
            .add("Manually trace the accumulator after each input token in the visible sample.")
            .add("Check whether your code ever reads the numbers after n.");
        root.put("nextStep", "Make the visible sample pass before trying hidden tests again.");
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
