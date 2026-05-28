package com.hackerprank.problems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("validFixtureNames")
    void validFixturesPassGeneratedProblemValidation(String fixtureName) throws Exception {
        GeneratedProblemSpec spec = readFixture(fixtureName);

        GeneratedProblemValidationReport report = validator.validate(spec);

        assertEquals("VALIDATED", report.status());
        assertTrue(report.summary().contains("Python/Java"));
        assertTrue(spec.generationMetadata().parametersJson().contains("array indexing"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFixtureNames")
    void invalidFixturesFailWithExpectedContractReason(String fixtureName) throws Exception {
        GeneratedProblemFixture fixture = readRawFixture(fixtureName);
        GeneratedProblemSpec spec = fixture.toSpec();

        assertTrue(
            fixture.expectedFailureMessage() != null && !fixture.expectedFailureMessage().isBlank(),
            "invalid fixtures must declare expectedFailureMessage"
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> validator.validate(spec)
        );

        assertTrue(exception.getMessage().contains(fixture.expectedFailureMessage()));
    }

    private GeneratedProblemSpec readFixture(String fixtureName) throws IOException {
        return readRawFixture(fixtureName).toSpec();
    }

    private GeneratedProblemFixture readRawFixture(String fixtureName) throws IOException {
        ClassPathResource resource = new ClassPathResource(FIXTURE_ROOT + fixtureName);
        return objectMapper.readValue(resource.getInputStream(), GeneratedProblemFixture.class);
    }

    private static Stream<String> validFixtureNames() throws IOException {
        return fixtureNames("valid-*.json");
    }

    private static Stream<String> invalidFixtureNames() throws IOException {
        return fixtureNames("invalid-*.json");
    }

    private static Stream<String> fixtureNames(String pattern) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        return Arrays.stream(resolver.getResources("classpath:" + FIXTURE_ROOT + pattern))
            .map(resource -> resource.getFilename())
            .filter(Objects::nonNull)
            .sorted();
    }

    private record GeneratedProblemFixture(
        String topic,
        String difficulty,
        String expectedFailureMessage,
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
