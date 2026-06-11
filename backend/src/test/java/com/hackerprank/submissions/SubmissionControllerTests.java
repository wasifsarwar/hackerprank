package com.hackerprank.submissions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void storesTutorFollowUpMessagesForPersistedSubmission() throws Exception {
        String submissionId = wrongAnswerSubmissionId();

        mockMvc.perform(post("/api/submissions/" + submissionId + "/tutor/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Can you explain the mismatch?\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.submissionId").value(submissionId))
            .andExpect(jsonPath("$.messages", hasSize(2)))
            .andExpect(jsonPath("$.messages[0].role").value("user"))
            .andExpect(jsonPath("$.messages[0].content").value("Can you explain the mismatch?"))
            .andExpect(jsonPath("$.messages[1].role").value("assistant"))
            .andExpect(jsonPath("$.messages[1].provider").value("deterministic"))
            .andExpect(jsonPath("$.messages[1].content", containsString("visible mismatch")));

        mockMvc.perform(get("/api/submissions/" + submissionId + "/tutor/messages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].role").value("user"))
            .andExpect(jsonPath("$[1].role").value("assistant"));
    }

    @Test
    void rejectsBlankTutorFollowUpMessage() throws Exception {
        String submissionId = wrongAnswerSubmissionId();

        mockMvc.perform(post("/api/submissions/" + submissionId + "/tutor/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"   \"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForMissingTutorFollowUpSubmission() throws Exception {
        mockMvc.perform(post("/api/submissions/missing-submission/tutor/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Can you help?\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void solvedEndpointCountsOnlyHiddenTestAccepts() throws Exception {
        String acceptedCode = """
            {
              "problemId": "add-a-pair",
              "language": "python",
              "code": "import sys\\nnumbers = list(map(int, sys.stdin.read().strip().split()))\\nprint(numbers[0] + numbers[1])\\n",
              "runHiddenTests": %s
            }
            """;

        mockMvc.perform(post("/api/submissions/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(acceptedCode.formatted("false")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/submissions/solved"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@ == 'add-a-pair')]").isEmpty());

        mockMvc.perform(post("/api/submissions/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(acceptedCode.formatted("true")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/submissions/solved"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@ == 'add-a-pair')]").isNotEmpty());
    }

    private String wrongAnswerSubmissionId() throws Exception {
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
        return runBody.get("submissionId").asText();
    }
}
