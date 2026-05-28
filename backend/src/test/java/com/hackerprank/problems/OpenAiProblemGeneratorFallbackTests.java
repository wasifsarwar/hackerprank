package com.hackerprank.problems;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "hackerprank.runner.mode=local",
    "hackerprank.runner.workspace-root=target/test-submissions",
    "hackerprank.generator.provider=openai",
    "hackerprank.openai.api-key="
})
@AutoConfigureMockMvc
class OpenAiProblemGeneratorFallbackTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProblemDraftRepository draftRepository;

    @Test
    void fallsBackToDeterministicGenerationWhenOpenAiKeyIsMissing() throws Exception {
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
    }
}
