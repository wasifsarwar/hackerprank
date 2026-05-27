package com.hackerprank.problems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public class ProblemRepository {
    private final Map<String, Problem> problems;

    public ProblemRepository() {
        Map<String, Problem> seed = new LinkedHashMap<>();
        Problem addPair = addPairProblem();
        Problem frequency = frequencyProblem();
        seed.put(addPair.getId(), addPair);
        seed.put(frequency.getId(), frequency);
        this.problems = Collections.unmodifiableMap(seed);
    }

    public List<Problem> findAll() {
        return new ArrayList<>(problems.values());
    }

    public Optional<Problem> findById(String id) {
        return Optional.ofNullable(problems.get(id));
    }

    private Problem addPairProblem() {
        Map<String, String> starterCode = new LinkedHashMap<>();
        starterCode.put("python",
            "import sys\n\n" +
            "def main():\n" +
            "    numbers = list(map(int, sys.stdin.read().strip().split()))\n" +
            "    # TODO: print the sum of the two numbers\n" +
            "    print(0)\n\n" +
            "if __name__ == \"__main__\":\n" +
            "    main()\n");
        starterCode.put("java",
            "import java.util.*;\n\n" +
            "public class Main {\n" +
            "    public static void main(String[] args) {\n" +
            "        Scanner scanner = new Scanner(System.in);\n" +
            "        int a = scanner.nextInt();\n" +
            "        int b = scanner.nextInt();\n" +
            "        // TODO: print the sum of a and b\n" +
            "        System.out.println(0);\n" +
            "    }\n" +
            "}\n");

        return new Problem(
            "add-a-pair",
            "Add a Pair",
            "Easy",
            Arrays.asList("Warmup", "Input/Output"),
            "Given two integers, print their sum. This tiny warmup makes sure your code can read stdin and write stdout.",
            "A single line containing two space-separated integers: a and b.",
            "Print a single integer: the sum of a and b.",
            Arrays.asList("-10,000 <= a, b <= 10,000"),
            Arrays.asList(
                new Example("3 5\n", "8\n", "3 plus 5 equals 8."),
                new Example("-2 9\n", "7\n", "-2 plus 9 equals 7.")
            ),
            Arrays.asList(
                new TestCase("Sample 1", "3 5\n", "8\n", false),
                new TestCase("Sample 2", "-2 9\n", "7\n", false),
                new TestCase("Hidden 1", "10000 -9999\n", "1\n", true),
                new TestCase("Hidden 2", "0 0\n", "0\n", true)
            ),
            starterCode
        );
    }

    private Problem frequencyProblem() {
        Map<String, String> starterCode = new LinkedHashMap<>();
        starterCode.put("python",
            "import sys\n\n" +
            "def main():\n" +
            "    tokens = sys.stdin.read().strip().split()\n" +
            "    n = int(tokens[0])\n" +
            "    words = tokens[1:]\n" +
            "    # TODO: print the word that appears most often\n" +
            "    print(words[0] if words else \"\")\n\n" +
            "if __name__ == \"__main__\":\n" +
            "    main()\n");
        starterCode.put("java",
            "import java.util.*;\n\n" +
            "public class Main {\n" +
            "    public static void main(String[] args) {\n" +
            "        Scanner scanner = new Scanner(System.in);\n" +
            "        int n = scanner.nextInt();\n" +
            "        String answer = \"\";\n" +
            "        for (int i = 0; i < n; i++) {\n" +
            "            String word = scanner.next();\n" +
            "            if (i == 0) {\n" +
            "                answer = word;\n" +
            "            }\n" +
            "        }\n" +
            "        // TODO: print the word with the highest frequency\n" +
            "        System.out.println(answer);\n" +
            "    }\n" +
            "}\n");

        return new Problem(
            "most-frequent-word",
            "Most Frequent Word",
            "Easy",
            Arrays.asList("Maps", "Counting"),
            "Given a list of lowercase words, print the word that appears the most often. If there is a tie, print the word that appears first among the tied words in the original list.",
            "The first line contains n. The second line contains n lowercase words separated by spaces.",
            "Print the most frequent word.",
            Arrays.asList("1 <= n <= 1000", "Each word contains only lowercase English letters."),
            Arrays.asList(
                new Example("5\nred blue red green blue\n", "red\n", "red and blue both appear twice, but red appears first."),
                new Example("4\ncode code test code\n", "code\n", "code appears three times.")
            ),
            Arrays.asList(
                new TestCase("Sample 1", "5\nred blue red green blue\n", "red\n", false),
                new TestCase("Sample 2", "4\ncode code test code\n", "code\n", false),
                new TestCase("Hidden 1", "6\na b c b c c\n", "c\n", true),
                new TestCase("Hidden 2", "3\nz y z\n", "z\n", true)
            ),
            starterCode
        );
    }
}
