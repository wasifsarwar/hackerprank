package com.hackerprank.submissions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

interface SandboxRunner {
    ProcessResult run(String language, List<String> command, Path workspace, String input, long timeoutMs)
        throws IOException, InterruptedException;
}
