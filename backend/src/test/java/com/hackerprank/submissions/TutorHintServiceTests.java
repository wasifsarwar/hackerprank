package com.hackerprank.submissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hackerprank.anthropic.AnthropicHttpResponse;
import com.hackerprank.anthropic.AnthropicTransport;
import com.hackerprank.openai.OpenAiHttpResponse;
import com.hackerprank.openai.OpenAiTransport;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class TutorHintServiceTests {
    private final TutorHintService tutorHintService = new TutorHintService();

    @Test
    void includesVisibleFailureDiffForWrongAnswers() {
        SubmissionDetail submission = submission(
            "WRONG_ANSWER",
            List.of(new TestCaseResult("sample one", false, false, "4\n", "5\n", "", false, 0, 25))
        );

        TutorHintResponse hint = tutorHintService.createHint(submission);
        String joinedHints = String.join("\n", hint.getHints());

        assertEquals("deterministic", hint.getProvider());
        assertEquals("nudge", hint.getLevel());
        assertTrue(hint.getSummary().contains("visible test"));
        assertTrue(joinedHints.contains("expected `4`"));
        assertTrue(joinedHints.contains("got `5`"));
    }

    @Test
    void keepsHiddenOnlyFailuresGeneric() {
        SubmissionDetail submission = submission(
            "WRONG_ANSWER",
            List.of(new TestCaseResult("hidden boundary", true, false, null, "secret actual output", "", false, 0, 31))
        );

        TutorHintResponse hint = tutorHintService.createHint(submission);
        String joined = hint.getSummary() + "\n" + String.join("\n", hint.getHints()) + "\n" + hint.getNextStep();

        assertTrue(joined.contains("hidden"));
        assertFalse(joined.contains("secret actual output"));
        assertFalse(joined.contains("hidden boundary"));
    }

    @Test
    void usesCompileOutputForCompileErrors() {
        SubmissionDetail submission = new SubmissionDetail(
            "submission-1",
            "problem-1",
            "Problem",
            "Easy",
            "java",
            "public class Main {}",
            false,
            "COMPILE_ERROR",
            0,
            2,
            "Main.java:3: error: cannot find symbol",
            Instant.EPOCH,
            List.of()
        );

        TutorHintResponse hint = tutorHintService.createHint(submission);
        String joinedHints = String.join("\n", hint.getHints());

        assertEquals("COMPILE_ERROR", hint.getStatus());
        assertTrue(joinedHints.contains("symbol"));
        assertTrue(joinedHints.contains("First diagnostic"));
    }

    @Test
    void usesOpenAiTutorWhenConfigured() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        TutorProperties tutorProperties = new TutorProperties();
        tutorProperties.setProvider("openai");
        OpenAiTutorProperties openAiProperties = new OpenAiTutorProperties();
        openAiProperties.setApiKey("sk-test");
        StubOpenAiTransport transport = new StubOpenAiTransport(openAiResponseBody(objectMapper));
        OpenAiTutorHintGenerator generator = new OpenAiTutorHintGenerator(openAiProperties, objectMapper, transport);
        TutorHintService service = new TutorHintService(tutorProperties, null, generator, null);

        TutorHintResponse hint = service.createHint(submission(
            "WRONG_ANSWER",
            List.of(new TestCaseResult("sample one", false, false, "4\n", "5\n", "", false, 0, 25))
        ));

        assertEquals("openai", hint.getProvider());
        assertEquals("nudge", hint.getLevel());
        assertTrue(hint.getSummary().contains("visible mismatch"));
    }

    @Test
    void usesAnthropicTutorWhenConfigured() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        TutorProperties tutorProperties = new TutorProperties();
        tutorProperties.setProvider("anthropic");
        AnthropicTutorProperties anthropicProperties = new AnthropicTutorProperties();
        anthropicProperties.setApiKey("sk-ant-test");
        StubAnthropicTransport transport = new StubAnthropicTransport(anthropicResponseBody(objectMapper));
        AnthropicTutorHintGenerator generator = new AnthropicTutorHintGenerator(anthropicProperties, objectMapper, transport);
        TutorHintService service = new TutorHintService(tutorProperties, null, null, generator);

        TutorHintResponse hint = service.createHint(submission(
            "WRONG_ANSWER",
            List.of(new TestCaseResult("sample one", false, false, "4\n", "5\n", "", false, 0, 25))
        ));

        assertEquals("anthropic", hint.getProvider());
        assertEquals("nudge", hint.getLevel());
        assertTrue(hint.getSummary().contains("visible mismatch"));
    }

    private SubmissionDetail submission(String status, List<TestCaseResult> results) {
        return new SubmissionDetail(
            "submission-1",
            "problem-1",
            "Problem",
            "Easy",
            "python",
            "print(0)",
            true,
            status,
            0,
            results.size(),
            "",
            Instant.EPOCH,
            results
        );
    }

    private static String openAiResponseBody(ObjectMapper objectMapper) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("level", "nudge");
        payload.put("summary", "The visible mismatch points at one branch of your logic.");
        payload.putArray("hints").add("Trace the sample by hand and compare state after each token.");
        payload.put("nextStep", "Make the visible sample pass before trying hidden tests again.");

        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "completed");
        ArrayNode output = response.putArray("output");
        ObjectNode message = output.addObject();
        message.put("type", "message");
        ArrayNode content = message.putArray("content");
        ObjectNode text = content.addObject();
        text.put("type", "output_text");
        text.put("text", objectMapper.writeValueAsString(payload));
        return objectMapper.writeValueAsString(response);
    }

    private static String anthropicResponseBody(ObjectMapper objectMapper) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("level", "nudge");
        payload.put("summary", "The visible mismatch points at one branch of your logic.");
        payload.putArray("hints").add("Trace the sample by hand and compare state after each token.");
        payload.put("nextStep", "Make the visible sample pass before trying hidden tests again.");

        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "message");
        response.put("role", "assistant");
        response.put("model", "claude-sonnet-4-6");
        response.put("stop_reason", "end_turn");
        ArrayNode content = response.putArray("content");
        ObjectNode text = content.addObject();
        text.put("type", "text");
        text.put("text", objectMapper.writeValueAsString(payload));
        return objectMapper.writeValueAsString(response);
    }

    private static class StubOpenAiTransport implements OpenAiTransport {
        private final String responseBody;

        StubOpenAiTransport(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public OpenAiHttpResponse post(URI uri, String apiKey, String requestBody, Duration timeout) {
            return new OpenAiHttpResponse(200, responseBody);
        }
    }

    private static class StubAnthropicTransport implements AnthropicTransport {
        private final String responseBody;

        StubAnthropicTransport(String responseBody) {
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
            return new AnthropicHttpResponse(200, responseBody);
        }
    }
}
