package com.hackerprank.problems;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    "hackerprank.runner.workspace-root=target/test-submissions"
})
@AutoConfigureMockMvc
class ProblemControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProblemRepository repository;

    @Autowired
    private ProblemDraftRepository draftRepository;

    @Test
    void generatesValidatedProblemAndStoresIt() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/problems/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"arrays\",\"difficulty\":\"Easy\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", startsWith("signal-peaks-")))
            .andExpect(jsonPath("$.title").value("Signal Peaks"))
            .andExpect(jsonPath("$.starterCode.python").exists())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue(repository.findById(body.get("id").asText()).isPresent());
    }

    @Test
    void createsValidatedDraftWithoutPublishingIt() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/problems/drafts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"stacks\",\"difficulty\":\"Medium\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", startsWith("draft-")))
            .andExpect(jsonPath("$.topic").value("stacks"))
            .andExpect(jsonPath("$.difficulty").value("Medium"))
            .andExpect(jsonPath("$.validationStatus").value("VALIDATED"))
            .andExpect(jsonPath("$.problem.id", startsWith("bracket-balance-")))
            .andExpect(jsonPath("$.problem.title").value("Bracket Balance"))
            .andExpect(jsonPath("$.referenceSolution").doesNotExist())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String draftId = body.get("id").asText();
        String problemId = body.get("problem").get("id").asText();

        assertTrue(draftRepository.findById(draftId).isPresent());
        assertFalse(repository.findById(problemId).isPresent());
    }

    @Test
    void publishesDraftAndRemovesItFromDraftStore() throws Exception {
        MvcResult draftResult = mockMvc.perform(post("/api/problems/drafts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"strings\",\"difficulty\":\"Hard\"}"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode draftBody = objectMapper.readTree(draftResult.getResponse().getContentAsString());
        String draftId = draftBody.get("id").asText();
        String problemId = draftBody.get("problem").get("id").asText();

        mockMvc.perform(post("/api/problems/drafts/" + draftId + "/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(problemId))
            .andExpect(jsonPath("$.title").value("First Solo Word"))
            .andExpect(jsonPath("$.difficulty").value("Hard"));

        assertTrue(repository.findById(problemId).isPresent());
        assertFalse(draftRepository.findById(draftId).isPresent());

        mockMvc.perform(get("/api/problems/drafts/" + draftId))
            .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnsupportedDifficulty() throws Exception {
        mockMvc.perform(post("/api/problems/drafts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"arrays\",\"difficulty\":\"Impossible\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Difficulty must be Easy, Medium, or Hard")));
    }
}
