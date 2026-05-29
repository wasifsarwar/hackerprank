package com.hackerprank.editor;

public record JavaCompletionItem(
    String label,
    String detail,
    String insertText,
    String kind
) {
}
