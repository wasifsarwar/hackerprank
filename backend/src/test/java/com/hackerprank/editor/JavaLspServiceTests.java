package com.hackerprank.editor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaLspServiceTests {
    @TempDir
    Path workspaceRoot;

    @Test
    void cleansUpSpawnedProcessWhenInitializationTimesOut() {
        JavaLspProperties properties = new JavaLspProperties();
        properties.setCommand("/bin/sh -c \"sleep 30\" {data}");
        properties.setWorkspaceRoot(workspaceRoot.toString());
        properties.setStartupTimeoutMs(100);
        JavaLspService service = new JavaLspService(properties, new ObjectMapper());
        JavaCompletionRequest request = new JavaCompletionRequest();
        request.setCode("class Main {}");
        request.setLineNumber(1);
        request.setColumn(1);

        JavaCompletionResponse response = service.complete(request);

        assertFalse(response.enabled());
        assertTrue(response.message().contains("textDocument") || response.message().contains("initialize"));
        assertFalse(service.hasRunningProcess());
    }

    @Test
    void documentVersionsAreMonotonicAndIndependentFromJsonRpcRequestIds() {
        JavaLspService service = new JavaLspService(new JavaLspProperties(), new ObjectMapper());

        assertEquals(1, service.nextDocumentVersion());
        assertEquals(2, service.nextDocumentVersion());
        assertEquals(3, service.nextDocumentVersion());
    }

    @Test
    void ignoresDiagnosticsOlderThanLatestDocumentVersion() {
        ObjectMapper objectMapper = new ObjectMapper();
        JavaLspService service = new JavaLspService(new JavaLspProperties(), objectMapper);
        String uri = "file:///workspace/project/src/main/java/Main.java";
        service.setLatestDocumentVersionForTesting(uri, 3);

        service.handleServerNotificationForTesting(diagnosticsNotification(objectMapper, uri, 3, "Current error"));
        service.handleServerNotificationForTesting(diagnosticsNotification(objectMapper, uri, 2, "Stale error"));

        assertEquals(1, service.diagnosticsForTesting(uri).size());
        assertEquals("Current error", service.diagnosticsForTesting(uri).getFirst().message());
    }

    private ObjectNode diagnosticsNotification(ObjectMapper objectMapper, String uri, int version, String message) {
        ObjectNode notification = objectMapper.createObjectNode();
        notification.put("jsonrpc", "2.0");
        notification.put("method", "textDocument/publishDiagnostics");
        ObjectNode params = notification.putObject("params");
        params.put("uri", uri);
        params.put("version", version);
        ObjectNode diagnostic = params.putArray("diagnostics").addObject();
        ObjectNode range = diagnostic.putObject("range");
        range.putObject("start").put("line", 0).put("character", 0);
        range.putObject("end").put("line", 0).put("character", 1);
        diagnostic.put("severity", 1);
        diagnostic.put("message", message);
        diagnostic.put("source", "Java");
        return notification;
    }
}
