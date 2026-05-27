package com.hackerprank.submissions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "hackerprank.runner.mode", havingValue = "local")
class LocalSandboxRunner implements SandboxRunner {
    private final ProcessRunner processRunner;

    LocalSandboxRunner(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    @Override
    public ProcessResult run(String language, List<String> command, Path workspace, String input, long timeoutMs)
        throws IOException, InterruptedException {
        return processRunner.run(command, workspace, input, timeoutMs);
    }
}
