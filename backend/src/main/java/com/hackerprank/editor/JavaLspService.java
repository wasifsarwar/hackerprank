package com.hackerprank.editor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

@Service
public class JavaLspService {
    private static final String DOCUMENT_LANGUAGE = "java";

    private final JavaLspProperties properties;
    private final ObjectMapper objectMapper;
    private final AtomicLong nextRequestId = new AtomicLong(1);
    private final Map<Long, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();

    private Process process;
    private BufferedOutputStream input;
    private Path projectRoot;
    private Path sourceFile;
    private String documentUri = "";
    private boolean initialized;
    private boolean documentOpened;
    private String disabledReason = "";

    public JavaLspService(JavaLspProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public synchronized JavaCompletionResponse status() {
        if (!properties.isEnabled()) {
            return JavaCompletionResponse.disabled("Java LSP is disabled.");
        }

        Optional<List<String>> command = resolveCommand();
        if (command.isEmpty()) {
            return JavaCompletionResponse.disabled("JDT LS was not found. Install `jdtls` or set HACKERPRANK_JAVA_LSP_COMMAND.");
        }

        return new JavaCompletionResponse(true, "jdtls", "JDT LS command is available.", List.of());
    }

    public synchronized JavaCompletionResponse complete(JavaCompletionRequest request) {
        if (!properties.isEnabled()) {
            return JavaCompletionResponse.disabled("Java LSP is disabled.");
        }

        try {
            ensureStarted();
            writeSource(request.getCode());
            syncDocument(request.getCode());
            JsonNode result = request("textDocument/completion", completionParams(request), properties.getRequestTimeoutMs());
            return JavaCompletionResponse.enabled(toCompletionItems(result));
        } catch (Exception exception) {
            disabledReason = exception.getMessage();
            return JavaCompletionResponse.disabled(disabledReason == null ? "JDT LS completion failed." : disabledReason);
        }
    }

    private void ensureStarted() throws IOException {
        if (initialized && process != null && process.isAlive()) {
            return;
        }

        Optional<List<String>> command = resolveCommand();
        if (command.isEmpty()) {
            throw new IOException("JDT LS was not found. Install `jdtls` or set HACKERPRANK_JAVA_LSP_COMMAND.");
        }

        prepareProject();
        ProcessBuilder processBuilder = new ProcessBuilder(commandWithData(command.get()));
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
        process = processBuilder.start();
        input = new BufferedOutputStream(process.getOutputStream());
        Thread reader = new Thread(() -> readMessages(process), "jdtls-lsp-reader");
        reader.setDaemon(true);
        reader.start();

        request("initialize", initializeParams(), Duration.ofSeconds(20).toMillis());
        notify("initialized", objectMapper.createObjectNode());
        initialized = true;
        documentOpened = false;
    }

    private void prepareProject() throws IOException {
        Path workspaceRoot = Path.of(properties.getWorkspaceRoot()).toAbsolutePath().normalize();
        projectRoot = workspaceRoot.resolve("project");
        sourceFile = projectRoot.resolve("src/main/java/Main.java");
        documentUri = sourceFile.toUri().toString();
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(
            projectRoot.resolve("pom.xml"),
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.hackerprank</groupId>
              <artifactId>editor-scratch</artifactId>
              <version>0.1.0</version>
              <properties>
                <maven.compiler.source>21</maven.compiler.source>
                <maven.compiler.target>21</maven.compiler.target>
              </properties>
            </project>
            """,
            StandardCharsets.UTF_8
        );
    }

    private void writeSource(String code) throws IOException {
        Files.writeString(sourceFile, code == null ? "" : code, StandardCharsets.UTF_8);
    }

    private void syncDocument(String code) throws IOException {
        ObjectNode params = objectMapper.createObjectNode();
        if (!documentOpened) {
            ObjectNode textDocument = params.putObject("textDocument");
            textDocument.put("uri", documentUri);
            textDocument.put("languageId", DOCUMENT_LANGUAGE);
            textDocument.put("version", 1);
            textDocument.put("text", code == null ? "" : code);
            notify("textDocument/didOpen", params);
            documentOpened = true;
            return;
        }

        ObjectNode textDocument = params.putObject("textDocument");
        textDocument.put("uri", documentUri);
        textDocument.put("version", (int) nextRequestId.get());
        ArrayNode changes = params.putArray("contentChanges");
        changes.addObject().put("text", code == null ? "" : code);
        notify("textDocument/didChange", params);
    }

    private ObjectNode initializeParams() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("processId", ProcessHandle.current().pid());
        params.put("rootUri", projectRoot.toUri().toString());
        ArrayNode workspaceFolders = objectMapper.createArrayNode();
        workspaceFolders.addObject()
            .put("uri", projectRoot.toUri().toString())
            .put("name", "hackerprank-editor");
        params.set("workspaceFolders", workspaceFolders);
        ObjectNode capabilities = params.putObject("capabilities");
        ObjectNode textDocument = capabilities.putObject("textDocument");
        ObjectNode completion = textDocument.putObject("completion");
        ObjectNode completionItem = completion.putObject("completionItem");
        completionItem.put("snippetSupport", true);
        completionItem.set("documentationFormat", objectMapper.createArrayNode().add("markdown").add("plaintext"));
        ObjectNode synchronization = textDocument.putObject("synchronization");
        synchronization.put("didSave", true);
        synchronization.put("dynamicRegistration", false);
        return params;
    }

    private ObjectNode completionParams(JavaCompletionRequest request) {
        ObjectNode params = objectMapper.createObjectNode();
        ObjectNode textDocument = params.putObject("textDocument");
        textDocument.put("uri", documentUri);
        ObjectNode position = params.putObject("position");
        position.put("line", Math.max(0, request.getLineNumber() - 1));
        position.put("character", Math.max(0, request.getColumn() - 1));
        ObjectNode context = params.putObject("context");
        context.put("triggerKind", 1);
        return params;
    }

    private JsonNode request(String method, JsonNode params, long timeoutMs) throws IOException {
        long id = nextRequestId.getAndIncrement();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        ObjectNode message = objectMapper.createObjectNode();
        message.put("jsonrpc", "2.0");
        message.put("id", id);
        message.put("method", method);
        message.set("params", params);
        writeMessage(message);

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            pendingRequests.remove(id);
            throw new IOException("JDT LS request timed out for " + method, exception);
        }
    }

    private void notify(String method, JsonNode params) throws IOException {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("jsonrpc", "2.0");
        message.put("method", method);
        message.set("params", params);
        writeMessage(message);
    }

    private synchronized void writeMessage(JsonNode message) throws IOException {
        byte[] body = objectMapper.writeValueAsBytes(message);
        String header = "Content-Length: " + body.length + "\r\n\r\n";
        input.write(header.getBytes(StandardCharsets.US_ASCII));
        input.write(body);
        input.flush();
    }

    private void readMessages(Process lspProcess) {
        try (BufferedInputStream output = new BufferedInputStream(lspProcess.getInputStream())) {
            while (lspProcess.isAlive()) {
                JsonNode message = readMessage(output);
                if (message == null) {
                    return;
                }
                JsonNode idNode = message.get("id");
                if (idNode != null && idNode.canConvertToLong() && (message.has("result") || message.has("error"))) {
                    CompletableFuture<JsonNode> future = pendingRequests.remove(idNode.asLong());
                    if (future != null) {
                        if (message.has("error")) {
                            future.completeExceptionally(new IOException(message.get("error").toString()));
                        } else {
                            future.complete(message.get("result"));
                        }
                    }
                } else if (idNode != null && idNode.canConvertToLong() && message.has("method")) {
                    respondToServerRequest(message);
                }
            }
        } catch (Exception ignored) {
            pendingRequests.values().forEach(future -> future.completeExceptionally(new IOException("JDT LS stopped.")));
            pendingRequests.clear();
        }
    }

    private void respondToServerRequest(JsonNode message) throws IOException {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", message.get("id"));
        String method = text(message, "method");
        if ("workspace/configuration".equals(method) || "workspace/workspaceFolders".equals(method)) {
            response.set("result", objectMapper.createArrayNode());
        } else {
            response.putNull("result");
        }
        writeMessage(response);
    }

    private JsonNode readMessage(BufferedInputStream output) throws IOException {
        int contentLength = -1;
        String line;
        while ((line = readHeaderLine(output)) != null) {
            if (line.isEmpty()) {
                break;
            }
            String lower = line.toLowerCase();
            if (lower.startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
            }
        }

        if (contentLength < 0) {
            return null;
        }

        byte[] body = output.readNBytes(contentLength);
        if (body.length != contentLength) {
            return null;
        }
        return objectMapper.readTree(body);
    }

    private String readHeaderLine(BufferedInputStream output) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int current;
        while ((current = output.read()) != -1) {
            if (current == '\n') {
                byte[] bytes = buffer.toByteArray();
                int length = bytes.length;
                if (length > 0 && bytes[length - 1] == '\r') {
                    length -= 1;
                }
                return new String(bytes, 0, length, StandardCharsets.US_ASCII);
            }
            buffer.write(current);
        }
        return null;
    }

    private List<JavaCompletionItem> toCompletionItems(JsonNode result) {
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
                String.valueOf(item.path("kind").asInt(0))
            ));
            if (completions.size() >= 60) {
                break;
            }
        }
        return completions;
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

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private Optional<List<String>> resolveCommand() {
        if (!properties.getCommand().isBlank()) {
            return Optional.of(splitCommand(properties.getCommand()));
        }
        for (String candidate : List.of("jdtls", "/opt/homebrew/bin/jdtls", "/usr/local/bin/jdtls")) {
            if (candidate.contains("/") && Files.isExecutable(Path.of(candidate))) {
                return Optional.of(List.of(candidate));
            }
            if (!candidate.contains("/") && executableOnPath(candidate)) {
                return Optional.of(List.of(candidate));
            }
        }
        return Optional.empty();
    }

    private List<String> commandWithData(List<String> command) throws IOException {
        Path dataDir = Path.of(properties.getWorkspaceRoot()).toAbsolutePath().normalize().resolve("data");
        Files.createDirectories(dataDir);
        List<String> expanded = new ArrayList<>();
        boolean usedPlaceholder = false;
        for (String part : command) {
            if (part.contains("{data}")) {
                expanded.add(part.replace("{data}", dataDir.toString()));
                usedPlaceholder = true;
            } else {
                expanded.add(part);
            }
        }
        if (!usedPlaceholder) {
            expanded.add("-data");
            expanded.add(dataDir.toString());
        }
        return expanded;
    }

    private boolean executableOnPath(String executable) {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return false;
        }
        for (String entry : path.split(System.getProperty("path.separator"))) {
            if (Files.isExecutable(Path.of(entry, executable))) {
                return true;
            }
        }
        return false;
    }

    private List<String> splitCommand(String command) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < command.length(); index += 1) {
            char character = command.charAt(index);
            if (character == '"') {
                quoted = !quoted;
            } else if (Character.isWhitespace(character) && !quoted) {
                if (!current.isEmpty()) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(character);
            }
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts;
    }
}
