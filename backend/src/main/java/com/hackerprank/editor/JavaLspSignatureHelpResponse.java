package com.hackerprank.editor;

import java.util.List;

public record JavaLspSignatureHelpResponse(
    boolean enabled,
    String source,
    String message,
    int activeSignature,
    int activeParameter,
    List<JavaLspSignature> signatures
) {
    static JavaLspSignatureHelpResponse disabled(String message) {
        return new JavaLspSignatureHelpResponse(false, "jdtls", message, 0, 0, List.of());
    }

    static JavaLspSignatureHelpResponse enabled(int activeSignature, int activeParameter, List<JavaLspSignature> signatures) {
        return new JavaLspSignatureHelpResponse(true, "jdtls", "", activeSignature, activeParameter, signatures);
    }
}
