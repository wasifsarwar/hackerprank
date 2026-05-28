package com.hackerprank.problems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

@SpringBootTest(properties = {
    "hackerprank.runner.mode=local",
    "hackerprank.runner.workspace-root=target/test-submissions"
})
class GeneratedProblemFixtureValidationTests {
    private static final String FIXTURE_ROOT = "generated-problems/";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GeneratedProblemValidator validator;

    @ParameterizedTest
    @ValueSource(strings = {
        "valid-edge-window-sum.json"
    })
    void validFixturesPassGeneratedProblemValidation(String fixtureName) throws Exception {
        GeneratedProblemSpec spec = readFixture(fixtureName);

        GeneratedProblemValidationReport report = validator.validate(spec);

        assertEquals("VALIDATED", report.status());
        assertTrue(report.summary().contains("Python/Java"));
        assertTrue(spec.generationMetadata().parametersJson().contains("array indexing"));
    }

    @ParameterizedTest
    @CsvSource({
        "invalid-reference-wrong-answer.json,reference solution returned WRONG_ANSWER",
        "invalid-missing-java-reference.json,referenceSolutions.java is required",
        "invalid-missing-hidden-tests.json,problem.testCases needs at least one hidden test"
    })
    void invalidFixturesFailWithExpectedContractReason(String fixtureName, String expectedMessage) throws Exception {
        GeneratedProblemSpec spec = readFixture(fixtureName);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> validator.validate(spec)
        );

        assertTrue(exception.getMessage().contains(expectedMessage));
    }

    private GeneratedProblemSpec readFixture(String fixtureName) throws IOException {
        ClassPathResource resource = new ClassPathResource(FIXTURE_ROOT + fixtureName);
        GeneratedProblemFixture fixture = objectMapper.readValue(resource.getInputStream(), GeneratedProblemFixture.class);
        return fixture.toSpec();
    }

    private record GeneratedProblemFixture(
        String topic,
        String difficulty,
        ProblemFixture problem,
        Map<String, String> referenceSolutions,
        MetadataFixture generationMetadata
    ) {
        private GeneratedProblemSpec toSpec() {
            return new GeneratedProblemSpec(
                topic,
                difficulty,
                problem.toProblem(),
                referenceSolutions,
                generationMetadata.toMetadata()
            );
        }
    }

    private record ProblemFixture(
        String id,
        String title,
        String difficulty,
        List<String> tags,
        String description,
        String inputFormat,
        String outputFormat,
        List<String> constraints,
        List<ExampleFixture> examples,
        List<TestCaseFixture> testCases,
        Map<String, String> starterCode
    ) {
        private Problem toProblem() {
            return new Problem(
                id,
                title,
                difficulty,
                tags,
                description,
                inputFormat,
                outputFormat,
                constraints,
                examples.stream().map(ExampleFixture::toExample).toList(),
                testCases.stream().map(TestCaseFixture::toTestCase).toList(),
                starterCode
            );
        }
    }

    private record ExampleFixture(String input, String output, String explanation) {
        private Example toExample() {
            return new Example(input, output, explanation);
        }
    }

    private record TestCaseFixture(String name, String input, String expectedOutput, boolean hidden) {
        private TestCase toTestCase() {
            return new TestCase(name, input, expectedOutput, hidden);
        }
    }

    private record MetadataFixture(
        String provider,
        String modelId,
        String promptVersion,
        String promptText,
        String parametersJson,
        String intendedTechnique
    ) {
        private GenerationMetadata toMetadata() {
            return new GenerationMetadata(
                provider,
                modelId,
                promptVersion,
                promptText,
                parametersJson,
                "PENDING",
                "",
                "",
                intendedTechnique
            );
        }
    }
}
