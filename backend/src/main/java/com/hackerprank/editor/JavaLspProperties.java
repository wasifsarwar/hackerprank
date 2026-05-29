package com.hackerprank.editor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hackerprank.editor.java-lsp")
class JavaLspProperties {
    private boolean enabled = true;
    private String command = "";
    private String workspaceRoot = ".hackerprank-jdtls";
    private long requestTimeoutMs = 6000;
    private long startupTimeoutMs = 20000;
    private long diagnosticsSettleMs = 1200;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command == null ? "" : command;
    }

    public String getWorkspaceRoot() {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(String workspaceRoot) {
        this.workspaceRoot = workspaceRoot == null || workspaceRoot.isBlank() ? ".hackerprank-jdtls" : workspaceRoot;
    }

    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = Math.max(1000, requestTimeoutMs);
    }

    public long getStartupTimeoutMs() {
        return startupTimeoutMs;
    }

    public void setStartupTimeoutMs(long startupTimeoutMs) {
        this.startupTimeoutMs = Math.max(100, startupTimeoutMs);
    }

    public long getDiagnosticsSettleMs() {
        return diagnosticsSettleMs;
    }

    public void setDiagnosticsSettleMs(long diagnosticsSettleMs) {
        this.diagnosticsSettleMs = Math.max(0, diagnosticsSettleMs);
    }
}
