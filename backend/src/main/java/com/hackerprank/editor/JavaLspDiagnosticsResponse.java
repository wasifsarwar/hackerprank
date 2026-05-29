package com.hackerprank.editor;

import java.util.List;

public record JavaLspDiagnosticsResponse(
    boolean enabled,
    String source,
    String message,
    List<JavaLspDiagnostic> diagnostics
) {
    static JavaLspDiagnosticsResponse disabled(String message) {
        return new JavaLspDiagnosticsResponse(false, "jdtls", message, List.of());
    }

    static JavaLspDiagnosticsResponse enabled(List<JavaLspDiagnostic> diagnostics) {
        return new JavaLspDiagnosticsResponse(true, "jdtls", "", diagnostics);
    }
}
