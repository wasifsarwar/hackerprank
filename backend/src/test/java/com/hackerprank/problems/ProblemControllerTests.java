package com.hackerprank.problems;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Autowired
    private GenerationAttemptRepository attemptRepository;

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
            .andExpect(jsonPath("$.generationMetadata.provider").value("deterministic"))
            .andExpect(jsonPath("$.generationMetadata.modelId").value("template-v1"))
            .andExpect(jsonPath("$.generationMetadata.validationSummary", containsString("Python/Java")))
            .andExpect(jsonPath("$.generationMetadata.parametersJson").doesNotExist())
            .andExpect(jsonPath("$.generationMetadata.validationErrors").doesNotExist())
            .andExpect(jsonPath("$.quality.status").value("VALIDATED"))
            .andExpect(jsonPath("$.generationAttempt.outcome").value("DRAFTED"))
            .andExpect(jsonPath("$.generationAttempt.feedbackTags").isArray())
            .andExpect(jsonPath("$.quality.repairUsed").value(false))
            .andExpect(jsonPath("$.quality.exampleCount").value(2))
            .andExpect(jsonPath("$.quality.visibleTestCount").value(2))
            .andExpect(jsonPath("$.quality.hiddenTestCount").value(4))
            .andExpect(jsonPath("$.quality.totalTestCount").value(6))
            .andExpect(jsonPath("$.quality.checks[0].label").value("Schema"))
            .andExpect(jsonPath("$.quality.checks[0].status").value("PASSED"))
            .andExpect(jsonPath("$.problem.id", startsWith("bracket-balance-")))
            .andExpect(jsonPath("$.problem.title").value("Bracket Balance"))
            .andExpect(jsonPath("$.referenceSolution").doesNotExist())
            .andExpect(jsonPath("$.referenceSolutions").doesNotExist())
            .andExpect(jsonPath("$.quality.validationErrors").doesNotExist())
            .andExpect(jsonPath("$.quality.promptText").doesNotExist())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String draftId = body.get("id").asText();
        String problemId = body.get("problem").get("id").asText();

        assertTrue(draftRepository.findById(draftId).isPresent());
        ProblemDraft persistedDraft = draftRepository.findById(draftId).orElseThrow();
        assertEquals("deterministic", persistedDraft.getGenerationMetadata().provider());
        assertTrue(persistedDraft.getReferenceSolutions().containsKey("python"));
        assertTrue(persistedDraft.getReferenceSolutions().containsKey("java"));
        assertTrue(attemptRepository.findByDraftId(draftId).isPresent());
        assertFalse(repository.findById(problemId).isPresent());
    }

    @Test
    void recordsDraftFeedback() throws Exception {
        MvcResult draftResult = mockMvc.perform(post("/api/problems/drafts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"arrays\",\"difficulty\":\"Easy\"}"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode draftBody = objectMapper.readTree(draftResult.getResponse().getContentAsString());
        String draftId = draftBody.get("id").asText();

        mockMvc.perform(post("/api/problems/drafts/" + draftId + "/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tags": ["Too easy", "Needs edge cases"],
                      "notes": "Make the examples more interview-like."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome").value("DRAFTED"))
            .andExpect(jsonPath("$.feedbackTags[0]").value("Needs edge cases"))
            .andExpect(jsonPath("$.feedbackTags[1]").value("Too easy"))
            .andExpect(jsonPath("$.feedbackNotes").value("Make the examples more interview-like."));

        GenerationAttempt attempt = attemptRepository.findByDraftId(draftId).orElseThrow();
        assertEquals("Make the examples more interview-like.", attempt.getFeedbackNotes());
        assertEquals(2, attempt.getFeedbackTags().size());
    }

    @Test
    void regeneratesDraftFromFeedbackAndDiscardsPreviousDraft() throws Exception {
        MvcResult draftResult = mockMvc.perform(post("/api/problems/drafts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "topic": "arrays",
                      "difficulty": "Medium",
                      "targetConcepts": ["prefix sums"],
                      "constraintsNotes": "Prefer realistic input names.",
                      "interviewStyle": "Practical"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode draftBody = objectMapper.readTree(draftResult.getResponse().getContentAsString());
        String previousDraftId = draftBody.get("id").asText();

        MvcResult regeneratedResult = mockMvc.perform(post("/api/problems/drafts/" + previousDraftId + "/regenerate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "action": "Add edge cases",
                      "tags": ["Needs edge cases"],
                      "notes": "Use a less template-like setup."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", startsWith("draft-")))
            .andExpect(jsonPath("$.id").value(not(previousDraftId)))
            .andExpect(jsonPath("$.generationAttempt.outcome").value("DRAFTED"))
            .andReturn();

        JsonNode regeneratedBody = objectMapper.readTree(regeneratedResult.getResponse().getContentAsString());
        String nextDraftId = regeneratedBody.get("id").asText();

        assertFalse(draftRepository.findById(previousDraftId).isPresent());
        assertTrue(draftRepository.findById(nextDraftId).isPresent());
        assertEquals("REGENERATED", attemptRepository.findByDraftId(previousDraftId).orElseThrow().getOutcome());

        ProblemDraft nextDraft = draftRepository.findById(nextDraftId).orElseThrow();
        JsonNode parameters = objectMapper.readTree(nextDraft.getGenerationMetadata().parametersJson());
        assertEquals("arrays", parameters.get("topic").asText());
        assertEquals("Medium", parameters.get("difficulty").asText());
        assertEquals("Practical", parameters.get("interviewStyle").asText());
        assertTrue(parameters.get("constraintsNotes").asText().contains("Regeneration action: Add edge cases"));
        assertTrue(parameters.get("constraintsNotes").asText().contains("Draft feedback tags: Needs edge cases"));
    }

    @Test
    void preservesGeneratorControlsInDraftMetadata() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/problems/drafts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "topic": "arrays",
                      "difficulty": "Medium",
                      "targetConcepts": ["two pointers", "prefix sums"],
                      "constraintsNotes": "Include at least one boundary-heavy case.",
                      "interviewStyle": "Edge-case heavy"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.generationMetadata.provider").value("deterministic"))
            .andExpect(jsonPath("$.generationMetadata.parametersJson").doesNotExist())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        ProblemDraft persistedDraft = draftRepository.findById(body.get("id").asText()).orElseThrow();
        JsonNode persistedParameters = objectMapper.readTree(persistedDraft.getGenerationMetadata().parametersJson());
        assertEquals("arrays", persistedParameters.get("topic").asText());
        assertEquals("Medium", persistedParameters.get("difficulty").asText());
        assertEquals("two pointers", persistedParameters.get("targetConcepts").get(0).asText());
        assertEquals("prefix sums", persistedParameters.get("targetConcepts").get(1).asText());
        assertEquals("Include at least one boundary-heavy case.", persistedParameters.get("constraintsNotes").asText());
        assertEquals("Edge-case heavy", persistedParameters.get("interviewStyle").asText());
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
        assertEquals("PUBLISHED", attemptRepository.findByDraftId(draftId).orElseThrow().getOutcome());

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
