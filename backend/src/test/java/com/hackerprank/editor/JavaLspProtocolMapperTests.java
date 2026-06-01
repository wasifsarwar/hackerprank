package com.hackerprank.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class JavaLspProtocolMapperTests {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JavaLspProtocolMapper mapper = new JavaLspProtocolMapper(objectMapper);

    @Test
    void returnsConfigurationPlaceholderForEachRequestedItem() {
        ObjectNode message = objectMapper.createObjectNode();
        ObjectNode params = message.putObject("params");
        ArrayNode items = params.putArray("items");
        items.addObject().put("section", "java.format.settings.url");
        items.addObject().put("section", "java.completion.favoriteStaticMembers");

        ArrayNode result = mapper.emptyConfigurationResult(message);

        assertEquals(2, result.size());
        assertTrue(result.get(0).isNull());
        assertTrue(result.get(1).isNull());
    }

    @Test
    void mapsCompletionAdditionalTextEditsToMonacoCoordinates() {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode items = result.putArray("items");
        ObjectNode item = items.addObject();
        item.put("label", "HashMap - java.util");
        item.put("detail", "java.util.HashMap");
        item.put("kind", 7);
        ObjectNode textEdit = item.putObject("textEdit");
        textEdit.put("newText", "HashMap");
        ArrayNode additionalTextEdits = item.putArray("additionalTextEdits");
        ObjectNode importEdit = additionalTextEdits.addObject();
        importEdit.put("newText", "import java.util.HashMap;\n\n");
        ObjectNode range = importEdit.putObject("range");
        range.putObject("start").put("line", 0).put("character", 0);
        range.putObject("end").put("line", 0).put("character", 0);

        List<JavaCompletionItem> completions = mapper.toCompletionItems(result);

        assertEquals(1, completions.size());
        JavaCompletionItem completion = completions.get(0);
        assertEquals("HashMap - java.util", completion.label());
        assertEquals("HashMap", completion.insertText());
        assertEquals("7", completion.kind());
        assertEquals(1, completion.additionalTextEdits().size());
        JavaLspTextEdit edit = completion.additionalTextEdits().get(0);
        assertEquals(1, edit.startLineNumber());
        assertEquals(1, edit.startColumn());
        assertEquals(1, edit.endLineNumber());
        assertEquals(1, edit.endColumn());
        assertEquals("import java.util.HashMap;\n\n", edit.text());
    }

    @Test
    void capsCompletionItemsToKeepResponsesSmall() {
        ArrayNode result = objectMapper.createArrayNode();
        for (int index = 0; index < 75; index += 1) {
            result.addObject()
                .put("label", "item" + index)
                .put("insertText", "item" + index)
                .put("kind", 2);
        }

        assertEquals(60, mapper.toCompletionItems(result).size());
    }

    @Test
    void mapsPublishDiagnosticsToMonacoCoordinates() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("uri", "file:///tmp/Main.java");
        ArrayNode diagnostics = params.putArray("diagnostics");
        ObjectNode diagnostic = diagnostics.addObject();
        diagnostic.put("severity", 1);
        diagnostic.put("message", "The method nope() is undefined");
        diagnostic.put("source", "Java");
        diagnostic.put("code", "67108964");
        ObjectNode range = diagnostic.putObject("range");
        range.putObject("start").put("line", 3).put("character", 4);
        range.putObject("end").put("line", 3).put("character", 10);

        List<JavaLspDiagnostic> mappedDiagnostics = mapper.toDiagnostics(params);

        assertEquals(1, mappedDiagnostics.size());
        JavaLspDiagnostic mapped = mappedDiagnostics.get(0);
        assertEquals(4, mapped.startLineNumber());
        assertEquals(5, mapped.startColumn());
        assertEquals(4, mapped.endLineNumber());
        assertEquals(11, mapped.endColumn());
        assertEquals(1, mapped.severity());
        assertEquals("The method nope() is undefined", mapped.message());
        assertEquals("Java", mapped.source());
        assertEquals("67108964", mapped.code());
    }
}
