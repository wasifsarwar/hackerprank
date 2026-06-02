package com.hackerprank.problems;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProblemGeneratorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProblemGeneratorService.class);
    private static final Pattern NON_SLUG_CHARACTERS = Pattern.compile("[^a-z0-9]+");

    private final ProblemRepository problemRepository;
    private final ProblemDraftRepository draftRepository;
    private final GenerationAttemptRepository attemptRepository;
    private final GeneratedProblemValidator generatedProblemValidator;
    private final ProblemGenerationProperties generationProperties;
    private final OpenAiProblemGenerator openAiProblemGenerator;
    private final AnthropicProblemGenerator anthropicProblemGenerator;
    private final ObjectMapper objectMapper;

    public ProblemGeneratorService(
        ProblemRepository problemRepository,
        ProblemDraftRepository draftRepository,
        GenerationAttemptRepository attemptRepository,
        GeneratedProblemValidator generatedProblemValidator,
        ProblemGenerationProperties generationProperties,
        OpenAiProblemGenerator openAiProblemGenerator,
        AnthropicProblemGenerator anthropicProblemGenerator,
        ObjectMapper objectMapper
    ) {
        this.problemRepository = problemRepository;
        this.draftRepository = draftRepository;
        this.attemptRepository = attemptRepository;
        this.generatedProblemValidator = generatedProblemValidator;
        this.generationProperties = generationProperties;
        this.openAiProblemGenerator = openAiProblemGenerator;
        this.anthropicProblemGenerator = anthropicProblemGenerator;
        this.objectMapper = objectMapper;
    }

    public PublicProblem generate(GenerateProblemRequest request) {
        ProblemDraft draft = createDraftEntity(request);
        draftRepository.publishById(draft.getId());
        attemptRepository.updateOutcomeByDraftId(draft.getId(), "PUBLISHED");

        return PublicProblem.from(draft.getProblem());
    }

    public PublicProblemDraft createDraft(GenerateProblemRequest request) {
        ProblemDraft draft = createDraftEntity(request);
        return PublicProblemDraft.from(draft, attemptRepository.findByDraftId(draft.getId()).orElse(null));
    }

    public Optional<PublicProblemDraft> findDraft(String draftId) {
        return draftRepository.findById(draftId)
            .map(draft -> PublicProblemDraft.from(draft, attemptRepository.findByDraftId(draft.getId()).orElse(null)));
    }

    public Optional<PublicGenerationAttempt> saveDraftFeedback(String draftId, DraftFeedbackRequest request) {
        return draftRepository.findById(draftId)
            .map(draft -> saveDraftFeedback(draft, request));
    }

    public Optional<PublicProblemDraft> regenerateDraft(String draftId, RegenerateDraftRequest request) {
        return draftRepository.findById(draftId)
            .map(previousDraft -> {
                RegenerateDraftRequest safeRequest = request == null ? new RegenerateDraftRequest() : request;
                saveDraftFeedback(previousDraft, safeRequest);
                GenerateProblemRequest regenerationRequest = requestFromDraft(previousDraft, safeRequest);
                ProblemDraft nextDraft = createDraftEntity(regenerationRequest);
                attemptRepository.updateOutcomeByDraftId(previousDraft.getId(), "REGENERATED");
                draftRepository.deleteById(previousDraft.getId());
                return PublicProblemDraft.from(
                    nextDraft,
                    attemptRepository.findByDraftId(nextDraft.getId()).orElse(null)
                );
            });
    }

    public Optional<PublicProblem> publishDraft(String draftId) {
        return draftRepository.findById(draftId)
            .map(draft -> {
                ensureAttemptForDraft(draft);
                draftRepository.publishById(draft.getId());
                attemptRepository.updateOutcomeByDraftId(draft.getId(), "PUBLISHED");
                return PublicProblem.from(draft.getProblem());
            });
    }

    public boolean deleteDraft(String draftId) {
        return draftRepository.findById(draftId)
            .map(draft -> {
                ensureAttemptForDraft(draft);
                attemptRepository.updateOutcomeByDraftId(draftId, "DISCARDED");
                draftRepository.deleteById(draftId);
                return true;
            })
            .orElse(false);
    }

    private ProblemDraft createDraftEntity(GenerateProblemRequest request) {
        ValidatedGeneratedProblem generated = draftFor(request);
        GeneratedProblemSpec spec = generated.spec();
        GeneratedProblemValidationReport validationReport = generated.validationReport();
        GenerationMetadata generationMetadata = spec.generationMetadata().withValidation(validationReport);

        ProblemDraft draft = new ProblemDraft(
            uniqueDraftId(),
            spec.topic(),
            spec.problem().getDifficulty(),
            spec.problem(),
            spec.referenceSolutions(),
            validationReport.status(),
            generationMetadata,
            Instant.now()
        );

        ProblemDraft savedDraft = draftRepository.save(draft);
        attemptRepository.save(attemptFor(savedDraft));
        return savedDraft;
    }

    private GenerationAttempt ensureAttemptForDraft(ProblemDraft draft) {
        return attemptRepository.findByDraftId(draft.getId())
            .orElseGet(() -> attemptRepository.save(attemptFor(draft)));
    }

    private PublicGenerationAttempt saveDraftFeedback(ProblemDraft draft, DraftFeedbackRequest request) {
        DraftFeedbackRequest safeRequest = request == null ? new DraftFeedbackRequest() : request;
        ensureAttemptForDraft(draft);
        return attemptRepository
            .replaceFeedbackByDraftId(draft.getId(), safeRequest.getTags(), safeRequest.getNotes())
            .map(PublicGenerationAttempt::from)
            .orElseThrow(() -> new IllegalStateException("Could not record draft feedback"));
    }

    private GenerationAttempt attemptFor(ProblemDraft draft) {
        GenerationMetadata metadata = draft.getGenerationMetadata() == null
            ? GenerationMetadata.empty()
            : draft.getGenerationMetadata();
        Instant now = Instant.now();
        return new GenerationAttempt(
            uniqueAttemptId(),
            draft.getId(),
            draft.getProblem().getId(),
            metadata.provider(),
            metadata.modelId(),
            metadata.promptVersion(),
            draft.getTopic(),
            draft.getDifficulty(),
            "DRAFTED",
            List.of(),
            "",
            usageMetricsFor(draft, metadata),
            now,
            now
        );
    }

    private GenerationUsageMetrics usageMetricsFor(ProblemDraft draft, GenerationMetadata metadata) {
        String promptText = metadata == null ? "" : metadata.promptText();
        String responseText = generatedResponseText(draft);
        return GenerationUsageEstimator.from(promptText, responseText);
    }

    private String generatedResponseText(ProblemDraft draft) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "problem",
                draft.getProblem(),
                "referenceSolutions",
                draft.getReferenceSolutions()
            ));
        } catch (Exception exception) {
            LOGGER.warn("Could not serialize generated draft response for usage metrics", exception);
            return "";
        }
    }

    private GenerateProblemRequest requestFromDraft(ProblemDraft draft, RegenerateDraftRequest request) {
        GenerateProblemRequest nextRequest = new GenerateProblemRequest();
        GenerationRequestParameters parameters = parametersFrom(draft.getGenerationMetadata());

        nextRequest.setTopic(parameters.topic().isBlank() ? draft.getTopic() : parameters.topic());
        nextRequest.setDifficulty(parameters.difficulty().isBlank() ? draft.getDifficulty() : parameters.difficulty());
        nextRequest.setTargetConcepts(parameters.targetConcepts());
        nextRequest.setInterviewStyle(parameters.interviewStyle().isBlank() ? "Classic" : parameters.interviewStyle());
        nextRequest.setConstraintsNotes(regenerationConstraints(parameters.constraintsNotes(), request));
        return nextRequest;
    }

    private GenerationRequestParameters parametersFrom(GenerationMetadata metadata) {
        if (metadata == null || metadata.parametersJson() == null || metadata.parametersJson().isBlank()) {
            return new GenerationRequestParameters("", "", List.of(), "", "");
        }

        try {
            JsonNode root = objectMapper.readTree(metadata.parametersJson());
            return new GenerationRequestParameters(
                textValue(root, "topic"),
                textValue(root, "difficulty"),
                stringList(root.get("targetConcepts")),
                textValue(root, "constraintsNotes"),
                textValue(root, "interviewStyle")
            );
        } catch (Exception exception) {
            LOGGER.warn("Could not parse generation parameters for draft regeneration", exception);
            return new GenerationRequestParameters("", "", List.of(), "", "");
        }
    }

    private String regenerationConstraints(String currentNotes, RegenerateDraftRequest request) {
        List<String> parts = new ArrayList<>();
        String normalizedCurrentNotes = normalizeText(currentNotes);
        if (!normalizedCurrentNotes.isBlank()) {
            parts.add(normalizedCurrentNotes);
        }

        String action = normalizeText(request.getAction());
        if (!action.isBlank()) {
            parts.add("Regeneration action: " + action);
        }

        List<String> tags = normalizeTargetConcepts(request.getTags());
        if (!tags.isEmpty()) {
            parts.add("Draft feedback tags: " + String.join(", ", tags));
        }

        String notes = normalizeText(request.getNotes());
        if (!notes.isBlank()) {
            parts.add("Draft feedback notes: " + notes);
        }

        return String.join("\n", parts);
    }

    private String textValue(JsonNode root, String fieldName) {
        JsonNode value = root == null ? null : root.get(fieldName);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private List<String> stringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            String text = value == null || value.isNull() ? "" : value.asText("");
            if (!text.isBlank()) {
                values.add(text.trim());
            }
        });
        return List.copyOf(values);
    }

    private ValidatedGeneratedProblem draftFor(GenerateProblemRequest request) {
        NormalizedGenerationRequest normalizedRequest = normalizeRequest(request);

        if (generationProperties.useOpenAi()) {
            Optional<ValidatedGeneratedProblem> generated = tryOpenAiDraft(normalizedRequest);
            if (generated.isPresent()) {
                return generated.get();
            }
        }

        if (generationProperties.useAnthropic()) {
            Optional<ValidatedGeneratedProblem> generated = tryAnthropicDraft(normalizedRequest);
            if (generated.isPresent()) {
                return generated.get();
            }
        }

        GeneratedProblemSpec deterministic = deterministicDraftFor(normalizedRequest);
        return new ValidatedGeneratedProblem(deterministic, generatedProblemValidator.validate(deterministic));
    }

    private Optional<ValidatedGeneratedProblem> tryOpenAiDraft(NormalizedGenerationRequest request) {
        if (!openAiProblemGenerator.isConfigured()) {
            LOGGER.info("OpenAI problem generation requested without an API key; using deterministic fallback");
            return Optional.empty();
        }

        try {
            GeneratedProblemSpec generated = withUniqueProblemId(openAiProblemGenerator.generate(
                request.topic(),
                request.difficulty(),
                request.targetConcepts(),
                request.constraintsNotes(),
                request.interviewStyle()
            ));
            GeneratedProblemValidationReport validationReport = generatedProblemValidator.validate(generated);
            return Optional.of(new ValidatedGeneratedProblem(generated, validationReport));
        } catch (OpenAiProblemGenerationException exception) {
            LOGGER.warn("OpenAI problem generation failed; using deterministic fallback", exception);
            return Optional.empty();
        } catch (IllegalStateException exception) {
            LOGGER.warn(
                "OpenAI problem generation did not pass validation; attempting one repair: {}",
                exception.getMessage()
            );
            return tryRepairOpenAiDraft(request, exception.getMessage());
        }
    }

    private Optional<ValidatedGeneratedProblem> tryRepairOpenAiDraft(
        NormalizedGenerationRequest request,
        String validationError
    ) {
        try {
            GeneratedProblemSpec repaired = withUniqueProblemId(openAiProblemGenerator.repair(
                request.topic(),
                request.difficulty(),
                request.targetConcepts(),
                request.constraintsNotes(),
                request.interviewStyle(),
                validationError
            ));
            GeneratedProblemValidationReport validationReport = generatedProblemValidator.validate(repaired);
            return Optional.of(new ValidatedGeneratedProblem(repaired, validationReport));
        } catch (OpenAiProblemGenerationException exception) {
            LOGGER.warn("OpenAI problem repair failed; using deterministic fallback", exception);
            return Optional.empty();
        } catch (IllegalStateException exception) {
            LOGGER.warn(
                "OpenAI problem repair did not pass validation; using deterministic fallback: {}",
                exception.getMessage()
            );
            return Optional.empty();
        }
    }

    private Optional<ValidatedGeneratedProblem> tryAnthropicDraft(NormalizedGenerationRequest request) {
        if (!anthropicProblemGenerator.isConfigured()) {
            LOGGER.info("Anthropic problem generation requested without an API key; using deterministic fallback");
            return Optional.empty();
        }

        try {
            GeneratedProblemSpec generated = withUniqueProblemId(anthropicProblemGenerator.generate(
                request.topic(),
                request.difficulty(),
                request.targetConcepts(),
                request.constraintsNotes(),
                request.interviewStyle()
            ));
            GeneratedProblemValidationReport validationReport = generatedProblemValidator.validate(generated);
            return Optional.of(new ValidatedGeneratedProblem(generated, validationReport));
        } catch (AnthropicProblemGenerationException exception) {
            LOGGER.warn("Anthropic problem generation failed; using deterministic fallback", exception);
            return Optional.empty();
        } catch (IllegalStateException exception) {
            LOGGER.warn(
                "Anthropic problem generation did not pass validation; attempting one repair: {}",
                exception.getMessage()
            );
            return tryRepairAnthropicDraft(request, exception.getMessage());
        }
    }

    private Optional<ValidatedGeneratedProblem> tryRepairAnthropicDraft(
        NormalizedGenerationRequest request,
        String validationError
    ) {
        try {
            GeneratedProblemSpec repaired = withUniqueProblemId(anthropicProblemGenerator.repair(
                request.topic(),
                request.difficulty(),
                request.targetConcepts(),
                request.constraintsNotes(),
                request.interviewStyle(),
                validationError
            ));
            GeneratedProblemValidationReport validationReport = generatedProblemValidator.validate(repaired);
            return Optional.of(new ValidatedGeneratedProblem(repaired, validationReport));
        } catch (AnthropicProblemGenerationException exception) {
            LOGGER.warn("Anthropic problem repair failed; using deterministic fallback", exception);
            return Optional.empty();
        } catch (IllegalStateException exception) {
            LOGGER.warn(
                "Anthropic problem repair did not pass validation; using deterministic fallback: {}",
                exception.getMessage()
            );
            return Optional.empty();
        }
    }

    private GeneratedProblemSpec deterministicDraftFor(NormalizedGenerationRequest normalizedRequest) {
        String selectionText = deterministicSelectionText(normalizedRequest);
        if (containsAny(selectionText, "stack", "bracket", "parentheses")) {
            return bracketBalanceProblem(normalizedRequest);
        }

        if (containsAny(selectionText, "sliding", "window", "two pointer", "two-pointer", "scan")) {
            return signalPeaksProblem(normalizedRequest);
        }

        if (containsAny(selectionText, "string", "map", "hash", "count", "anagram", "word", "frequency")) {
            return firstSoloWordProblem(normalizedRequest);
        }

        return signalPeaksProblem(normalizedRequest);
    }

    private GeneratedProblemSpec withUniqueProblemId(GeneratedProblemSpec spec) {
        Problem current = spec.problem();
        Problem problem = new Problem(
            uniqueId(slugBase(current.getId(), current.getTitle())),
            current.getTitle(),
            current.getDifficulty(),
            current.getTags(),
            current.getScenario(),
            current.getTask(),
            current.getJavaSignature(),
            current.getPythonSignature(),
            current.getDescription(),
            current.getInputFormat(),
            current.getOutputFormat(),
            current.getConstraints(),
            current.getExamples(),
            current.getTestCases(),
            current.getStarterCode()
        );

        return new GeneratedProblemSpec(
            spec.topic(),
            spec.difficulty(),
            problem,
            spec.referenceSolutions(),
            spec.generationMetadata()
        );
    }

    private record ValidatedGeneratedProblem(
        GeneratedProblemSpec spec,
        GeneratedProblemValidationReport validationReport
    ) {
    }

    private GeneratedProblemSpec signalPeaksProblem(NormalizedGenerationRequest request) {
        Map<String, String> starterCode = new LinkedHashMap<>();
        starterCode.put("python", """
            import sys

            def main():
                tokens = list(map(int, sys.stdin.read().strip().split()))
                n = tokens[0] if tokens else 0
                readings = tokens[1:1 + n]
                print(count_peaks(readings))

            def count_peaks(readings):
                # TODO: count readings that are greater than both neighbors
                return 0

            if __name__ == "__main__":
                main()
            """);
        starterCode.put("java", """
            import java.util.*;

            public class Main {
                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    int n = scanner.hasNextInt() ? scanner.nextInt() : 0;
                    int[] readings = new int[n];
                    for (int i = 0; i < n; i++) {
                        readings[i] = scanner.nextInt();
                    }
                    System.out.println(countPeaks(readings));
                }

                static int countPeaks(int[] readings) {
                    // TODO: count readings that are greater than both neighbors
                    return 0;
                }
            }
            """);

        Problem problem = new Problem(
            uniqueId("signal-peaks"),
            "Signal Peaks",
            request.difficulty(),
            Arrays.asList("Arrays", "Scanning"),
            "A field operations team reviews telemetry from a line of connected safety devices after an incident response. Each device reports one numeric signal strength for a short time window, and isolated jumps can indicate a local event worth reviewing before video is archived.",
            "Given the ordered signal readings, count how many internal readings are strict peaks. A reading is a peak only when it is greater than both immediate neighbors; the first and last readings do not have two neighbors and never count.",
            "static int countPeaks(int[] readings)",
            "def count_peaks(readings):",
            "A sensor line reports n readings. A reading is a peak if it is strictly greater than the reading immediately before it and immediately after it. The first and last readings never count as peaks.",
            "The first line contains n. The second line contains n space-separated integers.",
            "Print a single integer: the number of peak readings.",
            Arrays.asList("1 <= n <= 100,000", "-100,000 <= reading <= 100,000"),
            Arrays.asList(
                new Example("5\n1 3 2 5 4\n", "2\n", "3 and 5 are each greater than both neighboring readings."),
                new Example("6\n2 4 6 8 10 12\n", "0\n", "The readings rise steadily, so no internal reading is greater than its right neighbor.")
            ),
            Arrays.asList(
                new TestCase("Sample 1", "5\n1 3 2 5 4\n", "2\n", false),
                new TestCase("Sample 2", "6\n2 4 6 8 10 12\n", "0\n", false),
                new TestCase("Hidden 1", "7\n9 1 8 2 7 3 6\n", "2\n", true),
                new TestCase("Hidden 2", "8\n1 2 1 2 1 2 1 2\n", "3\n", true),
                new TestCase("Hidden 3", "1\n42\n", "0\n", true)
            ),
            starterCode
        );

        Map<String, String> referenceSolutions = new LinkedHashMap<>();
        referenceSolutions.put("python", """
            import sys

            tokens = list(map(int, sys.stdin.read().strip().split()))
            n = tokens[0] if tokens else 0
            readings = tokens[1:1 + n]
            total = 0
            for i in range(1, len(readings) - 1):
                if readings[i] > readings[i - 1] and readings[i] > readings[i + 1]:
                    total += 1
            print(total)
            """);
        referenceSolutions.put("java", """
            import java.util.*;

            public class Main {
                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    int n = scanner.hasNextInt() ? scanner.nextInt() : 0;
                    int[] readings = new int[n];
                    for (int i = 0; i < n; i++) {
                        readings[i] = scanner.nextInt();
                    }
                    int total = 0;
                    for (int i = 1; i < readings.length - 1; i++) {
                        if (readings[i] > readings[i - 1] && readings[i] > readings[i + 1]) {
                            total++;
                        }
                    }
                    System.out.println(total);
                }
            }
            """);

        return new GeneratedProblemSpec(
            request.topic(),
            request.difficulty(),
            problem,
            referenceSolutions,
            GenerationMetadata.deterministic(
                request.topic(),
                request.difficulty(),
                request.targetConcepts(),
                request.constraintsNotes(),
                request.interviewStyle(),
                "Single pass array scan comparing each internal reading to its immediate neighbors."
            )
        );
    }

    private GeneratedProblemSpec firstSoloWordProblem(NormalizedGenerationRequest request) {
        Map<String, String> starterCode = new LinkedHashMap<>();
        starterCode.put("python", """
            import sys

            def main():
                tokens = sys.stdin.read().strip().split()
                n = int(tokens[0]) if tokens else 0
                words = tokens[1:1 + n]
                print(first_solo_word(words))

            def first_solo_word(words):
                # TODO: return the first word that appears exactly once
                return "NONE"

            if __name__ == "__main__":
                main()
            """);
        starterCode.put("java", """
            import java.util.*;

            public class Main {
                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    int n = scanner.hasNextInt() ? scanner.nextInt() : 0;
                    String[] words = new String[n];
                    for (int i = 0; i < n; i++) {
                        words[i] = scanner.next();
                    }
                    System.out.println(firstSoloWord(words));
                }

                static String firstSoloWord(String[] words) {
                    // TODO: return the first word that appears exactly once
                    return "NONE";
                }
            }
            """);

        Problem problem = new Problem(
            uniqueId("first-solo-word"),
            "First Solo Word",
            request.difficulty(),
            Arrays.asList("Maps", "Counting", "Strings"),
            "An interview feedback system receives short normalized labels from multiple reviewers. Repeated labels usually represent common themes, but a single unique label can reveal the earliest unusual concern that should be escalated for manual review.",
            "Given the labels in arrival order, return the first word that appears exactly once in the entire list. If every word appears more than once, return NONE.",
            "static String firstSoloWord(String[] words)",
            "def first_solo_word(words):",
            "Given a list of lowercase words, print the first word that appears exactly once. If every word repeats, print NONE.",
            "The first line contains n. The second line contains n lowercase words separated by spaces.",
            "Print the first word with frequency one, or NONE if no such word exists.",
            Arrays.asList("1 <= n <= 100,000", "Each word contains only lowercase English letters."),
            Arrays.asList(
                new Example("6\nnova code nova lint test code\n", "lint\n", "lint and test each appear once, and lint appears first."),
                new Example("4\naa bb aa bb\n", "NONE\n", "Every word appears twice.")
            ),
            Arrays.asList(
                new TestCase("Sample 1", "6\nnova code nova lint test code\n", "lint\n", false),
                new TestCase("Sample 2", "4\naa bb aa bb\n", "NONE\n", false),
                new TestCase("Hidden 1", "5\nx y z y x\n", "z\n", true),
                new TestCase("Hidden 2", "3\none two three\n", "one\n", true),
                new TestCase("Hidden 3", "7\na b c a b c d\n", "d\n", true)
            ),
            starterCode
        );

        Map<String, String> referenceSolutions = new LinkedHashMap<>();
        referenceSolutions.put("python", """
            import sys

            tokens = sys.stdin.read().strip().split()
            n = int(tokens[0]) if tokens else 0
            words = tokens[1:1 + n]
            counts = {}
            for word in words:
                counts[word] = counts.get(word, 0) + 1
            for word in words:
                if counts[word] == 1:
                    print(word)
                    break
            else:
                print("NONE")
            """);
        referenceSolutions.put("java", """
            import java.util.*;

            public class Main {
                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    int n = scanner.hasNextInt() ? scanner.nextInt() : 0;
                    String[] words = new String[n];
                    Map<String, Integer> counts = new HashMap<>();
                    for (int i = 0; i < n; i++) {
                        words[i] = scanner.next();
                        counts.put(words[i], counts.getOrDefault(words[i], 0) + 1);
                    }
                    for (String word : words) {
                        if (counts.get(word) == 1) {
                            System.out.println(word);
                            return;
                        }
                    }
                    System.out.println("NONE");
                }
            }
            """);

        return new GeneratedProblemSpec(
            request.topic(),
            request.difficulty(),
            problem,
            referenceSolutions,
            GenerationMetadata.deterministic(
                request.topic(),
                request.difficulty(),
                request.targetConcepts(),
                request.constraintsNotes(),
                request.interviewStyle(),
                "Count each word with a hash map, then scan the original order to find the first count of one."
            )
        );
    }

    private GeneratedProblemSpec bracketBalanceProblem(NormalizedGenerationRequest request) {
        Map<String, String> starterCode = new LinkedHashMap<>();
        starterCode.put("python", """
            import sys

            def main():
                text = sys.stdin.read().strip()
                print("YES" if is_balanced(text) else "NO")

            def is_balanced(text):
                # TODO: return True if the brackets are balanced, otherwise False
                return False

            if __name__ == "__main__":
                main()
            """);
        starterCode.put("java", """
            import java.util.*;

            public class Main {
                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    String text = scanner.hasNext() ? scanner.next() : "";
                    System.out.println(isBalanced(text) ? "YES" : "NO");
                }

                static boolean isBalanced(String text) {
                    // TODO: return true if the brackets are balanced, otherwise false
                    return false;
                }
            }
            """);

        Problem problem = new Problem(
            uniqueId("bracket-balance"),
            "Bracket Balance",
            request.difficulty(),
            Arrays.asList("Stacks", "Parsing"),
            "A lightweight rules engine receives bracket-only filter expressions from a device configuration tool. Before sending a configuration downstream, the platform must reject expressions whose bracket pairs are nested or closed incorrectly.",
            "Given a string containing only bracket characters, decide whether every opening bracket is closed by the same bracket type and in the correct order. Return true for balanced expressions and false otherwise.",
            "static boolean isBalanced(String text)",
            "def is_balanced(text):",
            "Given a string containing only bracket characters, decide whether every opening bracket is closed by the same type of bracket in the correct order.",
            "A single line containing a string made of (, ), [, ], {, and } characters.",
            "Print YES if the bracket string is balanced. Otherwise, print NO.",
            Arrays.asList("1 <= length of string <= 100,000", "The string contains only bracket characters."),
            Arrays.asList(
                new Example("({[]})\n", "YES\n", "Each opener is closed in the correct nested order."),
                new Example("([)]\n", "NO\n", "The closing parenthesis appears before the square bracket is closed.")
            ),
            Arrays.asList(
                new TestCase("Sample 1", "({[]})\n", "YES\n", false),
                new TestCase("Sample 2", "([)]\n", "NO\n", false),
                new TestCase("Hidden 1", "(((())))\n", "YES\n", true),
                new TestCase("Hidden 2", "{[()()]}\n", "YES\n", true),
                new TestCase("Hidden 3", "((())\n", "NO\n", true),
                new TestCase("Hidden 4", "]\n", "NO\n", true)
            ),
            starterCode
        );

        Map<String, String> referenceSolutions = new LinkedHashMap<>();
        referenceSolutions.put("python", """
            import sys

            text = sys.stdin.read().strip()
            pairs = {")": "(", "]": "[", "}": "{"}
            openers = set(pairs.values())
            stack = []
            ok = True
            for char in text:
                if char in openers:
                    stack.append(char)
                elif char in pairs:
                    if not stack or stack.pop() != pairs[char]:
                        ok = False
                        break
            print("YES" if ok and not stack else "NO")
            """);
        referenceSolutions.put("java", """
            import java.util.*;

            public class Main {
                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    String text = scanner.hasNext() ? scanner.next() : "";
                    Map<Character, Character> pairs = Map.of(')', '(', ']', '[', '}', '{');
                    Deque<Character> stack = new ArrayDeque<>();
                    boolean ok = true;
                    for (char current : text.toCharArray()) {
                        if (pairs.containsValue(current)) {
                            stack.push(current);
                        } else if (pairs.containsKey(current)) {
                            if (stack.isEmpty() || !stack.pop().equals(pairs.get(current))) {
                                ok = false;
                                break;
                            }
                        }
                    }
                    System.out.println(ok && stack.isEmpty() ? "YES" : "NO");
                }
            }
            """);

        return new GeneratedProblemSpec(
            request.topic(),
            request.difficulty(),
            problem,
            referenceSolutions,
            GenerationMetadata.deterministic(
                request.topic(),
                request.difficulty(),
                request.targetConcepts(),
                request.constraintsNotes(),
                request.interviewStyle(),
                "Use a stack of opening brackets and match each closing bracket against the top."
            )
        );
    }

    private NormalizedGenerationRequest normalizeRequest(GenerateProblemRequest request) {
        if (request == null) {
            return new NormalizedGenerationRequest("arrays", "Easy", List.of(), "", "Classic");
        }

        return new NormalizedGenerationRequest(
            normalizeTopic(request.getTopic()),
            normalizeDifficulty(request.getDifficulty()),
            normalizeTargetConcepts(request.getTargetConcepts()),
            normalizeText(request.getConstraintsNotes()),
            normalizeInterviewStyle(request.getInterviewStyle())
        );
    }

    private List<String> normalizeTargetConcepts(List<String> targetConcepts) {
        if (targetConcepts == null || targetConcepts.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String concept : targetConcepts) {
            String value = normalizeText(concept);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }

        return List.copyOf(normalized);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeInterviewStyle(String interviewStyle) {
        String normalized = normalizeText(interviewStyle);
        return normalized.isBlank() ? "Classic" : normalized;
    }

    private String normalizeTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return "arrays";
        }

        return topic.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return "Easy";
        }

        return switch (difficulty.trim().toLowerCase(Locale.ROOT)) {
            case "easy" -> "Easy";
            case "medium" -> "Medium";
            case "hard" -> "Hard";
            default -> throw new IllegalArgumentException("Difficulty must be Easy, Medium, or Hard");
        };
    }

    private boolean containsAny(String topic, String... needles) {
        return Arrays.stream(needles).anyMatch(topic::contains);
    }

    private String deterministicSelectionText(NormalizedGenerationRequest request) {
        String concepts = String.join(" ", request.targetConcepts());
        return String.join(
            " ",
            request.topic(),
            concepts,
            request.constraintsNotes(),
            request.interviewStyle()
        ).toLowerCase(Locale.ROOT);
    }

    private String uniqueId(String base) {
        String slug = slugBase(base, "generated-problem");
        for (int attempt = 0; attempt < 5; attempt++) {
            String id = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
            if (!problemRepository.existsAnyById(id) && !draftRepository.existsByProblemId(id)) {
                return id;
            }
        }

        throw new IllegalStateException("Could not allocate a generated problem id");
    }

    private String uniqueDraftId() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String id = "draft-" + UUID.randomUUID().toString().substring(0, 8);
            if (draftRepository.findById(id).isEmpty()) {
                return id;
            }
        }

        throw new IllegalStateException("Could not allocate a generated draft id");
    }

    private String uniqueAttemptId() {
        return "attempt-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private String slugBase(String preferred, String fallback) {
        String source = preferred == null || preferred.isBlank() ? fallback : preferred;
        String slug = NON_SLUG_CHARACTERS.matcher(source.trim().toLowerCase(Locale.ROOT)).replaceAll("-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
        return slug.isBlank() ? "generated-problem" : slug;
    }

    private record NormalizedGenerationRequest(
        String topic,
        String difficulty,
        List<String> targetConcepts,
        String constraintsNotes,
        String interviewStyle
    ) {}

    private record GenerationRequestParameters(
        String topic,
        String difficulty,
        List<String> targetConcepts,
        String constraintsNotes,
        String interviewStyle
    ) {}
}
