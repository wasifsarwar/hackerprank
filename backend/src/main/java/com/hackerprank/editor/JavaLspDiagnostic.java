package com.hackerprank.editor;

public record JavaLspDiagnostic(
    int startLineNumber,
    int startColumn,
    int endLineNumber,
    int endColumn,
    int severity,
    String message,
    String source,
    String code
) {
}
