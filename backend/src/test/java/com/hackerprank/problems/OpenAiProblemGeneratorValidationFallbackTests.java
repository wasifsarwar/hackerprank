package com.hackerprank.problems;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "hackerprank.runner.mode=local",
    "hackerprank.runner.workspace-root=target/test-submissions",
    "hackerprank.generator.provider=openai",
    "hackerprank.openai.api-key=sk-test",
    "hackerprank.openai.responses-url=https://api.openai.test/v1/responses"
})
@AutoConfigureMockMvc
class OpenAiProblemGeneratorValidationFallbackTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProblemDraftRepository draftRepository;

    @Autowired
    private StubOpenAiTransport openAiTransport;

    @Test
    void fallsBackToDeterministicGenerationWhenOpenAiDraftFailsValidation() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/problems/drafts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"arrays\",\"difficulty\":\"Easy\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", startsWith("draft-")))
            .andExpect(jsonPath("$.problem.id", startsWith("signal-peaks-")))
            .andExpect(jsonPath("$.generationMetadata.provider").value("deterministic"))
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        ProblemDraft draft = draftRepository.findById(body.get("id").asText()).orElseThrow();
        assertEquals("deterministic", draft.getGenerationMetadata().provider());
        assertTrue(draft.getReferenceSolutions().containsKey("python"));
        assertTrue(draft.getReferenceSolutions().containsKey("java"));
        assertEquals(1, openAiTransport.calls);
    }

    @TestConfiguration
    static class StubOpenAiConfiguration {
        @Bean
        @Primary
        StubOpenAiTransport openAiTransport(ObjectMapper objectMapper) throws Exception {
            return new StubOpenAiTransport(objectMapper);
        }
    }

    static class StubOpenAiTransport implements OpenAiTransport {
        private final String responseBody;
        private int calls;

        StubOpenAiTransport(ObjectMapper objectMapper) throws Exception {
            this.responseBody = responseBody(objectMapper, problemJson(objectMapper));
        }

        @Override
        public OpenAiHttpResponse post(URI uri, String apiKey, String requestBody, Duration timeout) {
            calls++;
            return new OpenAiHttpResponse(200, responseBody);
        }

        private static String responseBody(ObjectMapper objectMapper, String outputText) throws Exception {
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

        private static String problemJson(ObjectMapper objectMapper) throws Exception {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("topic", "arrays");
            root.put("difficulty", "Easy");
            root.put("intendedTechnique", "Read all values and accumulate the total.");

            ObjectNode problem = root.putObject("problem");
            problem.put("id", "broken-openai-sum");
            problem.put("title", "Broken OpenAI Sum");
            problem.put("difficulty", "Easy");
            problem.putArray("tags").add("Arrays").add("Implementation");
            problem.put("description", "Given n integers, print their sum.");
            problem.put("inputFormat", "The first line contains n. The second line contains n space-separated integers.");
            problem.put("outputFormat", "Print one integer: the sum of the values.");
            problem.putArray("constraints").add("1 <= n <= 1000").add("-1000 <= value <= 1000");

            ArrayNode examples = problem.putArray("examples");
            examples.add(example(objectMapper, "3\n1 2 3\n", "6\n", "The values sum to 6."));
            examples.add(example(objectMapper, "2\n5 -1\n", "4\n", "The values sum to 4."));

            ArrayNode testCases = problem.putArray("testCases");
            testCases.add(testCase(objectMapper, "Sample 1", "3\n1 2 3\n", "6\n", false));
            testCases.add(testCase(objectMapper, "Sample 2", "2\n5 -1\n", "4\n", false));
            testCases.add(testCase(objectMapper, "Hidden 1", "1\n7\n", "7\n", true));
            testCases.add(testCase(objectMapper, "Hidden 2", "4\n1 1 1 1\n", "4\n", true));
            testCases.add(testCase(objectMapper, "Hidden 3", "3\n-2 2 5\n", "5\n", true));

            ObjectNode starterCode = problem.putObject("starterCode");
            starterCode.put("python", "import sys\n# TODO: sum values\nprint(0)\n");
            starterCode.put("java", "public class Main { public static void main(String[] args) { System.out.println(0); } }\n");

            ObjectNode referenceSolutions = root.putObject("referenceSolutions");
            referenceSolutions.put("python", "print(0)\n");
            referenceSolutions.put("java", "public class Main { public static void main(String[] args) { System.out.println(0); } }\n");

            return objectMapper.writeValueAsString(root);
        }

        private static ObjectNode example(ObjectMapper objectMapper, String input, String output, String explanation) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("input", input);
            node.put("output", output);
            node.put("explanation", explanation);
            return node;
        }

        private static ObjectNode testCase(
            ObjectMapper objectMapper,
            String name,
            String input,
            String expectedOutput,
            boolean hidden
        ) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", name);
            node.put("input", input);
            node.put("expectedOutput", expectedOutput);
            node.put("hidden", hidden);
            return node;
        }
    }
}
