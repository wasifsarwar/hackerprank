package com.hackerprank.submissions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "hackerprank.runner.mode=local",
    "hackerprank.runner.workspace-root=target/test-submissions"
})
class SubmissionServiceTests {
    @Autowired
    private SubmissionService submissionService;

    @Test
    void acceptsCorrectPythonSubmission() {
        SubmissionRequest request = new SubmissionRequest();
        request.setProblemId("add-a-pair");
        request.setLanguage("python");
        request.setRunHiddenTests(true);
        request.setCode(
            "import sys\n" +
            "numbers = list(map(int, sys.stdin.read().split()))\n" +
            "print(numbers[0] + numbers[1])\n"
        );

        SubmissionResult result = submissionService.run(request);

        assertEquals("ACCEPTED", result.getStatus());
        assertEquals(4, result.getPassedCount());
        assertEquals(4, result.getTotalCount());
    }

    @Test
    void acceptsCorrectJavaSubmission() {
        SubmissionRequest request = new SubmissionRequest();
        request.setProblemId("add-a-pair");
        request.setLanguage("java");
        request.setRunHiddenTests(true);
        request.setCode(
            "import java.util.*;\n" +
            "public class Main {\n" +
            "    public static void main(String[] args) {\n" +
            "        Scanner scanner = new Scanner(System.in);\n" +
            "        System.out.println(scanner.nextInt() + scanner.nextInt());\n" +
            "    }\n" +
            "}\n"
        );

        SubmissionResult result = submissionService.run(request);

        assertEquals("ACCEPTED", result.getStatus());
        assertEquals(4, result.getPassedCount());
        assertEquals(4, result.getTotalCount());
    }

    @Test
    void reportsWrongAnswerForIncorrectOutput() {
        SubmissionRequest request = new SubmissionRequest();
        request.setProblemId("add-a-pair");
        request.setLanguage("python");
        request.setRunHiddenTests(false);
        request.setCode("print(0)\n");

        SubmissionResult result = submissionService.run(request);

        assertEquals("WRONG_ANSWER", result.getStatus());
        assertEquals(0, result.getPassedCount());
        assertEquals(2, result.getTotalCount());
    }
}
