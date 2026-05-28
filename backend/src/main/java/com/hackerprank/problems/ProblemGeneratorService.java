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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProblemGeneratorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProblemGeneratorService.class);
    private static final Pattern NON_SLUG_CHARACTERS = Pattern.compile("[^a-z0-9]+");

    private final ProblemRepository problemRepository;
    private final ProblemDraftRepository draftRepository;
    private final GeneratedProblemValidator generatedProblemValidator;
    private final ProblemGenerationProperties generationProperties;
    private final OpenAiProblemGenerator openAiProblemGenerator;

    public ProblemGeneratorService(
        ProblemRepository problemRepository,
        ProblemDraftRepository draftRepository,
        GeneratedProblemValidator generatedProblemValidator,
        ProblemGenerationProperties generationProperties,
        OpenAiProblemGenerator openAiProblemGenerator
    ) {
        this.problemRepository = problemRepository;
        this.draftRepository = draftRepository;
        this.generatedProblemValidator = generatedProblemValidator;
        this.generationProperties = generationProperties;
        this.openAiProblemGenerator = openAiProblemGenerator;
    }

    public PublicProblem generate(GenerateProblemRequest request) {
        ProblemDraft draft = createDraftEntity(request);
        draftRepository.publishById(draft.getId());

        return PublicProblem.from(draft.getProblem());
    }

    public PublicProblemDraft createDraft(GenerateProblemRequest request) {
        return PublicProblemDraft.from(createDraftEntity(request));
    }

    public Optional<PublicProblemDraft> findDraft(String draftId) {
        return draftRepository.findById(draftId).map(PublicProblemDraft::from);
    }

    public Optional<PublicProblem> publishDraft(String draftId) {
        return draftRepository.findById(draftId)
            .map(draft -> {
                draftRepository.publishById(draft.getId());
                return PublicProblem.from(draft.getProblem());
            });
    }

    public boolean deleteDraft(String draftId) {
        boolean exists = draftRepository.findById(draftId).isPresent();
        if (exists) {
            draftRepository.deleteById(draftId);
        }
        return exists;
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

        return draftRepository.save(draft);
    }

    private ValidatedGeneratedProblem draftFor(GenerateProblemRequest request) {
        NormalizedGenerationRequest normalizedRequest = normalizeRequest(request);

        if (generationProperties.useOpenAi()) {
            Optional<ValidatedGeneratedProblem> generated = tryOpenAiDraft(normalizedRequest);
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
                "OpenAI problem generation did not pass validation; using deterministic fallback: {}",
                exception.getMessage()
            );
            return Optional.empty();
        }
    }

    private GeneratedProblemSpec deterministicDraftFor(NormalizedGenerationRequest normalizedRequest) {
        if (containsAny(normalizedRequest.topic(), "stack", "bracket", "parentheses")) {
            return bracketBalanceProblem(normalizedRequest);
        }

        if (containsAny(normalizedRequest.topic(), "string", "map", "hash", "count")) {
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
                # TODO: count readings that are greater than both neighbors
                print(0)

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
                    // TODO: count readings that are greater than both neighbors
                    System.out.println(0);
                }
            }
            """);

        Problem problem = new Problem(
            uniqueId("signal-peaks"),
            "Signal Peaks",
            request.difficulty(),
            Arrays.asList("Arrays", "Scanning"),
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
                # TODO: print the first word that appears exactly once
                print("NONE")

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
                    // TODO: print the first word that appears exactly once
                    System.out.println("NONE");
                }
            }
            """);

        Problem problem = new Problem(
            uniqueId("first-solo-word"),
            "First Solo Word",
            request.difficulty(),
            Arrays.asList("Maps", "Counting", "Strings"),
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
                # TODO: print YES if the brackets are balanced, otherwise NO
                print("NO")

            if __name__ == "__main__":
                main()
            """);
        starterCode.put("java", """
            import java.util.*;

            public class Main {
                public static void main(String[] args) {
                    Scanner scanner = new Scanner(System.in);
                    String text = scanner.hasNext() ? scanner.next() : "";
                    // TODO: print YES if the brackets are balanced, otherwise NO
                    System.out.println("NO");
                }
            }
            """);

        Problem problem = new Problem(
            uniqueId("bracket-balance"),
            "Bracket Balance",
            request.difficulty(),
            Arrays.asList("Stacks", "Parsing"),
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
}
