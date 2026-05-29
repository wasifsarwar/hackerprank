import type { Monaco } from "@monaco-editor/react";

let isConfigured = false;

interface SnippetSpec {
  detail: string;
  insertText: string;
  label: string;
}

interface HoverSpec {
  contents: string;
  word: string;
}

const javaSnippets: SnippetSpec[] = [
  {
    label: "main",
    detail: "public static void main",
    insertText: "public static void main(String[] args) {\n\t$0\n}"
  },
  {
    label: "class",
    detail: "Java class",
    insertText: "class ${1:Solution} {\n\t$0\n}"
  },
  {
    label: "fori",
    detail: "indexed for loop",
    insertText: "for (int ${1:i} = 0; ${1:i} < ${2:n}; ${1:i}++) {\n\t$0\n}"
  },
  {
    label: "if",
    detail: "if statement",
    insertText: "if (${1:condition}) {\n\t$0\n}"
  },
  {
    label: "HashMap",
    detail: "HashMap declaration",
    insertText: "Map<${1:String}, ${2:Integer}> ${3:map} = new HashMap<>();"
  },
  {
    label: "HashSet",
    detail: "HashSet declaration",
    insertText: "Set<${1:Integer}> ${2:seen} = new HashSet<>();"
  },
  {
    label: "ArrayList",
    detail: "ArrayList declaration",
    insertText: "List<${1:Integer}> ${2:items} = new ArrayList<>();"
  },
  {
    label: "PriorityQueue",
    detail: "PriorityQueue declaration",
    insertText: "PriorityQueue<${1:Integer}> ${2:heap} = new PriorityQueue<>();"
  },
  {
    label: "Deque",
    detail: "ArrayDeque declaration",
    insertText: "Deque<${1:Integer}> ${2:deque} = new ArrayDeque<>();"
  },
  {
    label: "readInts",
    detail: "stdin integer array parsing",
    insertText:
      "BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\nint[] nums = Arrays.stream(br.readLine().trim().split(\"\\\\s+\")).mapToInt(Integer::parseInt).toArray();"
  }
];

const pythonSnippets: SnippetSpec[] = [
  {
    label: "def",
    detail: "function definition",
    insertText: "def ${1:solve}(${2:args}):\n    $0"
  },
  {
    label: "main",
    detail: "main guard",
    insertText: "def main():\n    $0\n\nif __name__ == \"__main__\":\n    main()"
  },
  {
    label: "stdin",
    detail: "read stdin tokens",
    insertText: "import sys\n\ndata = sys.stdin.read().strip().split()\n$0"
  },
  {
    label: "Counter",
    detail: "collections.Counter",
    insertText: "from collections import Counter\n\n${1:counts} = Counter(${2:items})"
  },
  {
    label: "defaultdict",
    detail: "collections.defaultdict",
    insertText: "from collections import defaultdict\n\n${1:groups} = defaultdict(${2:list})"
  },
  {
    label: "deque",
    detail: "collections.deque",
    insertText: "from collections import deque\n\n${1:queue} = deque()"
  },
  {
    label: "heapq",
    detail: "min-heap helpers",
    insertText: "import heapq\n\n${1:heap} = []\nheapq.heappush(${1:heap}, ${2:value})"
  },
  {
    label: "fori",
    detail: "range loop",
    insertText: "for ${1:i} in range(${2:n}):\n    $0"
  }
];

const javaLibraryWords = [
  "Arrays",
  "Collections",
  "StringBuilder",
  "Map",
  "Set",
  "List",
  "Queue",
  "Deque",
  "HashMap",
  "HashSet",
  "ArrayList",
  "ArrayDeque",
  "PriorityQueue",
  "Comparator",
  "Integer",
  "Long",
  "Math",
  "BufferedReader",
  "InputStreamReader"
];

const javaHovers: HoverSpec[] = [
  {
    word: "HashMap",
    contents: "Average O(1) key lookup/update. Useful for frequency maps, indexes, and prefix-sum counts."
  },
  {
    word: "HashSet",
    contents: "Average O(1) membership checks. Useful for seen-state, duplicate detection, and two-sum style lookups."
  },
  {
    word: "PriorityQueue",
    contents: "Binary heap. Default is min-heap; pass a comparator for max-heap or custom ordering."
  },
  {
    word: "ArrayDeque",
    contents: "Fast double-ended queue. Prefer over Stack/LinkedList for stack and queue interview patterns."
  }
];

const pythonHovers: HoverSpec[] = [
  {
    word: "Counter",
    contents: "Dictionary subclass for frequency counting. Useful for anagrams, windows, and majority/frequency problems."
  },
  {
    word: "defaultdict",
    contents: "Dictionary with automatic default values. Useful for grouping, graph adjacency lists, and counters."
  },
  {
    word: "deque",
    contents: "Double-ended queue with O(1) append/pop from both ends. Useful for BFS and monotonic queues."
  },
  {
    word: "heapq",
    contents: "Python min-heap utilities. Push tuples like `(priority, value)` for custom ordering."
  }
];

interface EditorPosition {
  column: number;
  lineNumber: number;
}

interface WordRangeModel {
  getWordUntilPosition: (position: EditorPosition) => {
    endColumn: number;
    startColumn: number;
  };
}

interface HoverModel {
  getWordAtPosition: (position: EditorPosition) => { word: string } | null;
}

function rangeForWord(model: WordRangeModel, position: EditorPosition) {
  const word = model.getWordUntilPosition(position);
  return {
    endColumn: word.endColumn,
    endLineNumber: position.lineNumber,
    startColumn: word.startColumn,
    startLineNumber: position.lineNumber
  };
}

function snippetSuggestions(
  monaco: Monaco,
  model: WordRangeModel,
  position: EditorPosition,
  snippets: SnippetSpec[]
) {
  const range = rangeForWord(model, position);
  return snippets.map((snippet) => ({
    detail: snippet.detail,
    insertText: snippet.insertText,
    insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    kind: monaco.languages.CompletionItemKind.Snippet,
    label: snippet.label,
    range
  }));
}

function wordSuggestions(
  monaco: Monaco,
  model: WordRangeModel,
  position: EditorPosition,
  words: string[]
) {
  const range = rangeForWord(model, position);
  return words.map((word) => ({
    detail: "Java interview API",
    insertText: word,
    kind: monaco.languages.CompletionItemKind.Class,
    label: word,
    range
  }));
}

function registerHover(monaco: Monaco, language: string, hovers: HoverSpec[]) {
  monaco.languages.registerHoverProvider(language, {
    provideHover(model, position) {
      const word = (model as HoverModel).getWordAtPosition(position)?.word;
      const hover = hovers.find((item) => item.word === word);
      if (!hover) {
        return null;
      }

      return {
        contents: [{ value: `**${hover.word}**` }, { value: hover.contents }]
      };
    }
  });
}

export function configureMonacoIntelligence(monaco: Monaco) {
  if (isConfigured) {
    return;
  }

  isConfigured = true;
  monaco.languages.registerCompletionItemProvider("java", {
    triggerCharacters: [".", "(", "<"],
    provideCompletionItems(model, position) {
      return {
        suggestions: [
          ...snippetSuggestions(monaco, model, position, javaSnippets),
          ...wordSuggestions(monaco, model, position, javaLibraryWords)
        ]
      };
    }
  });

  monaco.languages.registerCompletionItemProvider("python", {
    triggerCharacters: [".", "(", "_"],
    provideCompletionItems(model, position) {
      return {
        suggestions: snippetSuggestions(monaco, model, position, pythonSnippets)
      };
    }
  });

  registerHover(monaco, "java", javaHovers);
  registerHover(monaco, "python", pythonHovers);
}
