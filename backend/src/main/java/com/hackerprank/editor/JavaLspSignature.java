package com.hackerprank.editor;

import java.util.List;

public record JavaLspSignature(
    String label,
    String documentation,
    List<String> parameters
) {
}
