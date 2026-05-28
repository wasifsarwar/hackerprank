package com.hackerprank.submissions;

import static org.hamcrest.Matchers.containsString;
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
    "hackerprank.runner.workspace-root=target/test-submissions"
})
@AutoConfigureMockMvc
class SubmissionControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsHintForPersistedWrongAnswer() throws Exception {
        MvcResult runResult = mockMvc.perform(post("/api/submissions/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "problemId": "add-a-pair",
                      "language": "python",
                      "code": "print(0)\\n",
                      "runHiddenTests": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.submissionId").exists())
            .andReturn();

        JsonNode runBody = objectMapper.readTree(runResult.getResponse().getContentAsString());
        String submissionId = runBody.get("submissionId").asText();

        mockMvc.perform(post("/api/submissions/" + submissionId + "/hint"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.submissionId").value(submissionId))
            .andExpect(jsonPath("$.provider").value("deterministic"))
            .andExpect(jsonPath("$.level").value("nudge"))
            .andExpect(jsonPath("$.summary", containsString("visible test")))
            .andExpect(jsonPath("$.hints[0]", containsString("expected")));
    }

    @Test
    void returnsNotFoundForMissingSubmissionHint() throws Exception {
        mockMvc.perform(post("/api/submissions/missing-submission/hint"))
            .andExpect(status().isNotFound());
    }
}
