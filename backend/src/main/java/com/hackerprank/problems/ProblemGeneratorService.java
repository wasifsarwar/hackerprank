package com.hackerprank.problems;

import com.hackerprank.submissions.SubmissionResult;
import com.hackerprank.submissions.SubmissionService;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class ProblemGeneratorService {
    private final ProblemRepository problemRepository;
    private final ProblemDraftRepository draftRepository;
    private final SubmissionService submissionService;

    public ProblemGeneratorService(
        ProblemRepository problemRepository,
        ProblemDraftRepository draftRepository,
        SubmissionService submissionService
    ) {
        this.problemRepository = problemRepository;
        this.draftRepository = draftRepository;
        this.submissionService = submissionService;
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
        GeneratedProblemDraft generated = draftFor(request);
        validateReferenceSolution(generated);

        ProblemDraft draft = new ProblemDraft(
            uniqueDraftId(),
            generated.topic(),
            generated.problem().getDifficulty(),
            generated.problem(),
            generated.referenceSolution(),
            "VALIDATED",
            Instant.now()
        );

        return draftRepository.save(draft);
    }

    private GeneratedProblemDraft draftFor(GenerateProblemRequest request) {
        String topic = normalizeTopic(request == null ? null : request.getTopic());
        String difficulty = normalizeDifficulty(request == null ? null : request.getDifficulty());

        if (containsAny(topic, "stack", "bracket", "parentheses")) {
            return bracketBalanceProblem(topic, difficulty);
        }

        if (containsAny(topic, "string", "map", "hash", "count")) {
            return firstSoloWordProblem(topic, difficulty);
        }

        return signalPeaksProblem(topic, difficulty);
    }

    private GeneratedProblemDraft signalPeaksProblem(String topic, String difficulty) {
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
            difficulty,
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

        return new GeneratedProblemDraft(topic, problem, """
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
    }

    private GeneratedProblemDraft firstSoloWordProblem(String topic, String difficulty) {
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
            difficulty,
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

        return new GeneratedProblemDraft(topic, problem, """
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
    }

    private GeneratedProblemDraft bracketBalanceProblem(String topic, String difficulty) {
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
            difficulty,
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

        return new GeneratedProblemDraft(topic, problem, """
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
    }

    private void validateReferenceSolution(GeneratedProblemDraft draft) {
        SubmissionResult result = submissionService.run(draft.problem(), "python", draft.referenceSolution(), true);
        if (!"ACCEPTED".equals(result.getStatus())) {
            throw new IllegalStateException("Generated problem failed validation with status " + result.getStatus());
        }
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
        for (int attempt = 0; attempt < 5; attempt++) {
            String id = base + "-" + UUID.randomUUID().toString().substring(0, 8);
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

    private record GeneratedProblemDraft(String topic, Problem problem, String referenceSolution) {
    }
}
