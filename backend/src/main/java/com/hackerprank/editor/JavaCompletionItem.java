package com.hackerprank.editor;

import java.util.List;

public record JavaCompletionItem(
    String label,
    String detail,
    String insertText,
    String kind,
    List<JavaLspTextEdit> additionalTextEdits
) {
}
