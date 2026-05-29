import type { Monaco } from "@monaco-editor/react";

let isConfigured = false;

interface EditorPosition {
  column: number;
  lineNumber: number;
}

interface CompletionModel {
  getLineContent: (lineNumber: number) => string;
  getWordUntilPosition: (position: EditorPosition) => {
    endColumn: number;
    startColumn: number;
    word: string;
  };
}

interface HoverModel {
  getWordAtPosition: (position: EditorPosition) => { word: string } | null;
}

interface SnippetSpec {
  detail: string;
  insertText: string;
  label: string;
}

interface ApiCompletionSpec {
  detail: string;
  insertText: string;
  label: string;
}

interface HoverSpec {
  contents: string;
  word: string;
}

const javaSnippets: SnippetSpec[] = [
  ["main", "public static void main", "public static void main(String[] args) {\n\t$0\n}"],
  ["class", "Java class", "class ${1:Solution} {\n\t$0\n}"],
  ["fori", "indexed for loop", "for (int ${1:i} = 0; ${1:i} < ${2:n}; ${1:i}++) {\n\t$0\n}"],
  ["foreach", "enhanced for loop", "for (${1:int} ${2:value} : ${3:values}) {\n\t$0\n}"],
  ["if", "if statement", "if (${1:condition}) {\n\t$0\n}"],
  ["else", "else branch", "else {\n\t$0\n}"],
  ["HashMap", "frequency/index map", "Map<${1:String}, ${2:Integer}> ${3:map} = new HashMap<>();"],
  ["HashSet", "seen set", "Set<${1:Integer}> ${2:seen} = new HashSet<>();"],
  ["ArrayList", "resizable list", "List<${1:Integer}> ${2:items} = new ArrayList<>();"],
  ["PriorityQueue", "min heap", "PriorityQueue<${1:Integer}> ${2:heap} = new PriorityQueue<>();"],
  ["maxHeap", "max heap", "PriorityQueue<${1:Integer}> ${2:heap} = new PriorityQueue<>(Comparator.reverseOrder());"],
  ["Deque", "queue or monotonic deque", "Deque<${1:Integer}> ${2:deque} = new ArrayDeque<>();"],
  [
    "readInts",
    "read a line of integers",
    "BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\nint[] nums = Arrays.stream(br.readLine().trim().split(\"\\\\s+\")).mapToInt(Integer::parseInt).toArray();"
  ],
  ["parseInt", "parse integer", "Integer.parseInt(${1:value})"],
  ["sortArray", "sort int array", "Arrays.sort(${1:nums});"],
  ["binarySearch", "binary search array", "int ${1:index} = Arrays.binarySearch(${2:nums}, ${3:target});"],
  ["StringBuilder", "string builder", "StringBuilder ${1:sb} = new StringBuilder();"],
  ["return", "return value", "return ${1:value};"]
].map(([label, detail, insertText]) => ({ detail, insertText, label }));

const pythonSnippets: SnippetSpec[] = [
  ["def", "function definition", "def ${1:solve}(${2:args}):\n    $0"],
  ["main", "main guard", "def main():\n    $0\n\nif __name__ == \"__main__\":\n    main()"],
  ["stdin", "read stdin tokens", "import sys\n\ndata = sys.stdin.read().strip().split()\n$0"],
  ["ints", "parse stdin integers", "nums = list(map(int, input().split()))"],
  ["Counter", "frequency counter", "from collections import Counter\n\n${1:counts} = Counter(${2:items})"],
  ["defaultdict", "grouping map", "from collections import defaultdict\n\n${1:groups} = defaultdict(${2:list})"],
  ["deque", "double-ended queue", "from collections import deque\n\n${1:queue} = deque()"],
  ["heapq", "min heap", "import heapq\n\n${1:heap} = []\nheapq.heappush(${1:heap}, ${2:value})"],
  ["fori", "range loop", "for ${1:i} in range(${2:n}):\n    $0"],
  ["enumerate", "indexed iteration", "for ${1:i}, ${2:value} in enumerate(${3:items}):\n    $0"],
  ["sortkey", "sort with key", "${1:items}.sort(key=lambda ${2:x}: ${3:x})"],
  ["lambda", "lambda expression", "lambda ${1:x}: ${2:x}"]
].map(([label, detail, insertText]) => ({ detail, insertText, label }));

const javaTypes: ApiCompletionSpec[] = [
  ["Arrays", "java.util.Arrays helper methods", "Arrays"],
  ["Collections", "java.util.Collections helper methods", "Collections"],
  ["StringBuilder", "mutable string builder", "StringBuilder"],
  ["Map", "key-value interface", "Map"],
  ["Set", "unique-value interface", "Set"],
  ["List", "ordered collection interface", "List"],
  ["Queue", "FIFO queue interface", "Queue"],
  ["Deque", "double-ended queue interface", "Deque"],
  ["HashMap", "hash-table map", "HashMap"],
  ["HashSet", "hash-table set", "HashSet"],
  ["ArrayList", "resizable array list", "ArrayList"],
  ["ArrayDeque", "fast stack/queue/deque", "ArrayDeque"],
  ["PriorityQueue", "binary heap", "PriorityQueue"],
  ["Comparator", "custom ordering", "Comparator"],
  ["Integer", "integer helpers", "Integer"],
  ["Long", "long helpers", "Long"],
  ["Math", "math helpers", "Math"],
  ["Scanner", "simple stdin scanner", "Scanner"],
  ["BufferedReader", "fast stdin reader", "BufferedReader"]
].map(([label, detail, insertText]) => ({ detail, insertText, label }));

const javaApiByReceiver: Record<string, ApiCompletionSpec[]> = {
  Arrays: [
    ["sort", "sort an array in place", "sort(${1:array})"],
    ["binarySearch", "binary search sorted array", "binarySearch(${1:array}, ${2:key})"],
    ["stream", "create stream from array", "stream(${1:array})"],
    ["fill", "fill array with value", "fill(${1:array}, ${2:value})"],
    ["copyOf", "copy array to length", "copyOf(${1:array}, ${2:newLength})"]
  ].map(([label, detail, insertText]) => ({ detail, insertText, label })),
  Collections: [
    ["sort", "sort a list in place", "sort(${1:list})"],
    ["reverse", "reverse a list in place", "reverse(${1:list})"],
    ["binarySearch", "binary search sorted list", "binarySearch(${1:list}, ${2:key})"],
    ["max", "maximum element", "max(${1:collection})"],
    ["min", "minimum element", "min(${1:collection})"]
  ].map(([label, detail, insertText]) => ({ detail, insertText, label })),
  Math: [
    ["max", "larger of two values", "max(${1:a}, ${2:b})"],
    ["min", "smaller of two values", "min(${1:a}, ${2:b})"],
    ["abs", "absolute value", "abs(${1:value})"],
    ["pow", "power", "pow(${1:base}, ${2:exp})"]
  ].map(([label, detail, insertText]) => ({ detail, insertText, label })),
  Integer: [["parseInt", "parse decimal integer", "parseInt(${1:value})"]].map(([label, detail, insertText]) => ({
    detail,
    insertText,
    label
  })),
  Long: [["parseLong", "parse decimal long", "parseLong(${1:value})"]].map(([label, detail, insertText]) => ({
    detail,
    insertText,
    label
  }))
};

const javaInstanceApis: ApiCompletionSpec[] = [
  ["add", "collection add", "add(${1:value})"],
  ["remove", "collection remove", "remove(${1:value})"],
  ["contains", "membership check", "contains(${1:value})"],
  ["get", "indexed/key lookup", "get(${1:key})"],
  ["put", "map insert/update", "put(${1:key}, ${2:value})"],
  ["getOrDefault", "map lookup with default", "getOrDefault(${1:key}, ${2:defaultValue})"],
  ["size", "collection size", "size()"],
  ["isEmpty", "emptiness check", "isEmpty()"],
  ["peek", "queue/deque peek", "peek()"],
  ["poll", "queue/deque pop front", "poll()"],
  ["push", "stack push", "push(${1:value})"],
  ["pop", "stack pop", "pop()"],
  ["append", "StringBuilder append", "append(${1:value})"],
  ["toString", "string representation", "toString()"]
].map(([label, detail, insertText]) => ({ detail, insertText, label }));

const pythonApisByReceiver: Record<string, ApiCompletionSpec[]> = {
  heapq: [
    ["heappush", "push onto heap", "heappush(${1:heap}, ${2:item})"],
    ["heappop", "pop smallest item", "heappop(${1:heap})"],
    ["heapify", "heapify list in place", "heapify(${1:items})"]
  ].map(([label, detail, insertText]) => ({ detail, insertText, label })),
  sys: [["stdin", "standard input stream", "stdin"]].map(([label, detail, insertText]) => ({ detail, insertText, label }))
};

const pythonInstanceApis: ApiCompletionSpec[] = [
  ["append", "list/deque append", "append(${1:value})"],
  ["appendleft", "deque append left", "appendleft(${1:value})"],
  ["pop", "remove last/right item", "pop()"],
  ["popleft", "deque remove left item", "popleft()"],
  ["sort", "sort list in place", "sort()"],
  ["get", "dict get with default", "get(${1:key}, ${2:default})"],
  ["items", "dict items", "items()"],
  ["keys", "dict keys", "keys()"],
  ["values", "dict values", "values()"]
].map(([label, detail, insertText]) => ({ detail, insertText, label }));

const javaHovers: HoverSpec[] = [
  ["HashMap", "Average O(1) key lookup/update. Useful for frequency maps, indexes, and prefix-sum counts."],
  ["HashSet", "Average O(1) membership checks. Useful for seen-state, duplicate detection, and two-sum lookups."],
  ["PriorityQueue", "Binary heap. Default is min-heap; pass a comparator for max-heap or custom ordering."],
  ["ArrayDeque", "Fast double-ended queue. Prefer over Stack/LinkedList for stack, queue, and monotonic deque patterns."],
  ["Arrays", "Static helpers for arrays: sort, binarySearch, fill, stream, copyOf."]
].map(([word, contents]) => ({ contents, word }));

const pythonHovers: HoverSpec[] = [
  ["Counter", "Dictionary subclass for frequency counting. Useful for anagrams, windows, and frequency problems."],
  ["defaultdict", "Dictionary with automatic defaults. Useful for grouping, graph adjacency lists, and counters."],
  ["deque", "Double-ended queue with O(1) append/pop from both ends. Useful for BFS and monotonic queues."],
  ["heapq", "Python min-heap utilities. Push tuples like `(priority, value)` for custom ordering."]
].map(([word, contents]) => ({ contents, word }));

function rangeForWord(model: CompletionModel, position: EditorPosition) {
  const word = model.getWordUntilPosition(position);
  return {
    endColumn: word.endColumn,
    endLineNumber: position.lineNumber,
    startColumn: word.startColumn,
    startLineNumber: position.lineNumber
  };
}

function rangeAfterDot(model: CompletionModel, position: EditorPosition) {
  const line = model.getLineContent(position.lineNumber);
  const dotColumn = line.lastIndexOf(".", position.column - 2) + 1;
  return {
    endColumn: position.column,
    endLineNumber: position.lineNumber,
    startColumn: dotColumn + 1,
    startLineNumber: position.lineNumber
  };
}

function receiverBeforeDot(model: CompletionModel, position: EditorPosition) {
  const linePrefix = model.getLineContent(position.lineNumber).slice(0, position.column - 1);
  const match = linePrefix.match(/([A-Za-z_][\w]*)\.[\w]*$/);
  return match?.[1] ?? "";
}

function startsWithDotContext(model: CompletionModel, position: EditorPosition) {
  return receiverBeforeDot(model, position).length > 0;
}

function snippetSuggestions(monaco: Monaco, model: CompletionModel, position: EditorPosition, snippets: SnippetSpec[]) {
  const range = rangeForWord(model, position);
  return snippets.map((snippet, index) => ({
    detail: snippet.detail,
    filterText: snippet.label,
    insertText: snippet.insertText,
    insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    kind: monaco.languages.CompletionItemKind.Snippet,
    label: snippet.label,
    range,
    sortText: `0_${String(index).padStart(2, "0")}_${snippet.label}`
  }));
}

function apiSuggestions(
  monaco: Monaco,
  range: ReturnType<typeof rangeForWord>,
  specs: ApiCompletionSpec[],
  group = "1"
) {
  return specs.map((item, index) => ({
    detail: item.detail,
    filterText: item.label,
    insertText: item.insertText,
    insertTextRules: item.insertText.includes("$")
      ? monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
      : undefined,
    kind: monaco.languages.CompletionItemKind.Method,
    label: item.label,
    range,
    sortText: `${group}_${String(index).padStart(2, "0")}_${item.label}`
  }));
}

function registerHover(monaco: Monaco, language: string, hovers: HoverSpec[]) {
  monaco.languages.registerHoverProvider(language, {
    provideHover(model, position) {
      const word = (model as HoverModel).getWordAtPosition(position)?.word;
      const hover = hovers.find((item) => item.word === word);
      return hover ? { contents: [{ value: `**${hover.word}**` }, { value: hover.contents }] } : null;
    }
  });
}

export function configureMonacoIntelligence(monaco: Monaco) {
  if (isConfigured) {
    return;
  }

  isConfigured = true;

  monaco.languages.registerCompletionItemProvider("java", {
    triggerCharacters: [".", "(", "<", " "],
    provideCompletionItems(model, position) {
      const typedModel = model as CompletionModel;
      if (startsWithDotContext(typedModel, position)) {
        const receiver = receiverBeforeDot(typedModel, position);
        const receiverApis = javaApiByReceiver[receiver] ?? javaInstanceApis;
        return {
          suggestions: apiSuggestions(monaco, rangeAfterDot(typedModel, position), receiverApis)
        };
      }

      return {
        suggestions: [
          ...snippetSuggestions(monaco, typedModel, position, javaSnippets),
          ...apiSuggestions(monaco, rangeForWord(typedModel, position), javaTypes, "1")
        ]
      };
    }
  });

  monaco.languages.registerCompletionItemProvider("python", {
    triggerCharacters: [".", "(", "_", " "],
    provideCompletionItems(model, position) {
      const typedModel = model as CompletionModel;
      if (startsWithDotContext(typedModel, position)) {
        const receiver = receiverBeforeDot(typedModel, position);
        const receiverApis = pythonApisByReceiver[receiver] ?? pythonInstanceApis;
        return {
          suggestions: apiSuggestions(monaco, rangeAfterDot(typedModel, position), receiverApis)
        };
      }

      return {
        suggestions: snippetSuggestions(monaco, typedModel, position, pythonSnippets)
      };
    }
  });

  registerHover(monaco, "java", javaHovers);
  registerHover(monaco, "python", pythonHovers);
}
