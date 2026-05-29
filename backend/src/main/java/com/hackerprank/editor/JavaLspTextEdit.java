package com.hackerprank.editor;

public record JavaLspTextEdit(
    int startLineNumber,
    int startColumn,
    int endLineNumber,
    int endColumn,
    String text
) {
}
