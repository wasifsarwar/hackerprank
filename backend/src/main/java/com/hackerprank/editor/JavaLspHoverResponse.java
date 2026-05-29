package com.hackerprank.editor;

public record JavaLspHoverResponse(
    boolean enabled,
    String source,
    String message,
    String contents
) {
    static JavaLspHoverResponse disabled(String message) {
        return new JavaLspHoverResponse(false, "jdtls", message, "");
    }

    static JavaLspHoverResponse enabled(String contents) {
        return new JavaLspHoverResponse(true, "jdtls", "", contents);
    }
}
