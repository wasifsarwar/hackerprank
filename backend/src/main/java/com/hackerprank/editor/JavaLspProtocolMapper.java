package com.hackerprank.editor;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

class JavaLspProtocolMapper {
    private final ObjectMapper objectMapper;

    JavaLspProtocolMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ArrayNode emptyConfigurationResult(JsonNode message) {
        ArrayNode result = objectMapper.createArrayNode();
        JsonNode items = message.path("params").path("items");
        if (!items.isArray()) {
            return result;
        }
        for (int index = 0; index < items.size(); index += 1) {
            result.addNull();
        }
        return result;
    }

    List<JavaCompletionItem> toCompletionItems(JsonNode result) {
        JsonNode items = result == null ? null : result.get("items");
        if (items == null && result != null && result.isArray()) {
            items = result;
        }
        if (items == null || !items.isArray()) {
            return List.of();
        }

        List<JavaCompletionItem> completions = new ArrayList<>();
        for (JsonNode item : items) {
            completions.add(new JavaCompletionItem(
                text(item, "label"),
                text(item, "detail"),
                insertText(item),
                String.valueOf(item.path("kind").asInt(0)),
                additionalTextEdits(item)
            ));
            if (completions.size() >= 60) {
                break;
            }
        }
        return completions;
    }

    JavaLspSignatureHelpResponse toSignatureHelp(JsonNode result) {
        if (result == null || result.isNull()) {
            return JavaLspSignatureHelpResponse.enabled(0, 0, List.of());
        }

        List<JavaLspSignature> signatures = new ArrayList<>();
        JsonNode signatureItems = result.path("signatures");
        if (signatureItems.isArray()) {
            for (JsonNode signature : signatureItems) {
                List<String> parameters = new ArrayList<>();
                JsonNode parameterItems = signature.path("parameters");
                if (parameterItems.isArray()) {
                    for (JsonNode parameter : parameterItems) {
                        parameters.add(parameterLabel(parameter.path("label")));
                    }
                }
                signatures.add(new JavaLspSignature(
                    text(signature, "label"),
                    markup(signature.get("documentation")),
                    parameters
                ));
                if (signatures.size() >= 12) {
                    break;
                }
            }
        }
        return JavaLspSignatureHelpResponse.enabled(
            result.path("activeSignature").asInt(0),
            result.path("activeParameter").asInt(0),
            signatures
        );
    }

    String hoverContents(JsonNode result) {
        if (result == null || result.isNull()) {
            return "";
        }
        return markup(result.get("contents"));
    }

    private String insertText(JsonNode item) {
        JsonNode textEdit = item.get("textEdit");
        if (textEdit != null && textEdit.has("newText")) {
            return textEdit.get("newText").asText();
        }
        if (item.has("insertText")) {
            return item.get("insertText").asText();
        }
        return text(item, "label");
    }

    private List<JavaLspTextEdit> additionalTextEdits(JsonNode item) {
        JsonNode edits = item.get("additionalTextEdits");
        if (edits == null || !edits.isArray()) {
            return List.of();
        }

        List<JavaLspTextEdit> mappedEdits = new ArrayList<>();
        for (JsonNode edit : edits) {
            JsonNode range = edit.path("range");
            JsonNode start = range.path("start");
            JsonNode end = range.path("end");
            mappedEdits.add(new JavaLspTextEdit(
                start.path("line").asInt(0) + 1,
                start.path("character").asInt(0) + 1,
                end.path("line").asInt(0) + 1,
                end.path("character").asInt(0) + 1,
                text(edit, "newText")
            ));
        }
        return mappedEdits;
    }

    private String parameterLabel(JsonNode label) {
        if (label == null || label.isNull()) {
            return "";
        }
        if (label.isArray() && label.size() == 2) {
            return label.get(0).asInt() + ":" + label.get(1).asInt();
        }
        return label.asText("");
    }

    private String markup(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode item : node) {
                String part = markup(item);
                if (!part.isBlank()) {
                    parts.add(part);
                }
            }
            return String.join("\n\n", parts);
        }
        if (node.has("language") && node.has("value")) {
            return "```" + text(node, "language") + "\n" + text(node, "value") + "\n```";
        }
        if (node.has("value")) {
            return node.get("value").asText("");
        }
        return node.toString();
    }

    String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }
}
