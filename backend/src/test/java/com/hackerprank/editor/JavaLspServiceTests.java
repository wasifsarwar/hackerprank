package com.hackerprank.editor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
