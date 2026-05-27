package com.hackerprank.submissions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "hackerprank.runner.mode", havingValue = "docker", matchIfMissing = true)
class DockerSandboxRunner implements SandboxRunner {
    private static final String WORKSPACE_MOUNT = "/workspace";

    private final ProcessRunner processRunner;
    private final String pythonImage;
    private final String javaImage;
    private final String cpus;
    private final String memory;
    private final String pidsLimit;
    private final long dockerGraceMs;

    DockerSandboxRunner(
        ProcessRunner processRunner,
        @Value("${hackerprank.runner.docker.python-image:python:3.12-alpine}") String pythonImage,
        @Value("${hackerprank.runner.docker.java-image:eclipse-temurin:21-jdk-alpine}") String javaImage,
        @Value("${hackerprank.runner.docker.cpus:1}") String cpus,
        @Value("${hackerprank.runner.docker.memory:256m}") String memory,
        @Value("${hackerprank.runner.docker.pids-limit:128}") String pidsLimit,
        @Value("${hackerprank.runner.docker.grace-ms:1000}") long dockerGraceMs
    ) {
        this.processRunner = processRunner;
        this.pythonImage = pythonImage;
        this.javaImage = javaImage;
        this.cpus = cpus;
        this.memory = memory;
        this.pidsLimit = pidsLimit;
        this.dockerGraceMs = dockerGraceMs;
    }

    @Override
    public ProcessResult run(String language, List<String> command, Path workspace, String input, long timeoutMs)
        throws IOException, InterruptedException {
        String containerName = "hackerprank-" + UUID.randomUUID();
        List<String> dockerCommand = buildDockerCommand(language, command, workspace, containerName);
        ProcessResult result = processRunner.run(dockerCommand, workspace, input, timeoutMs + dockerGraceMs);

        if (result.isTimedOut()) {
            forceRemoveContainer(containerName);
        }

        return result;
    }

    List<String> buildDockerCommand(String language, List<String> command, Path workspace, String containerName) {
        List<String> dockerCommand = new ArrayList<>();
        dockerCommand.add("docker");
        dockerCommand.add("run");
        dockerCommand.add("--rm");
        dockerCommand.add("--interactive");
        dockerCommand.add("--name");
        dockerCommand.add(containerName);
        dockerCommand.add("--pull");
        dockerCommand.add("missing");
        dockerCommand.add("--network");
        dockerCommand.add("none");
        dockerCommand.add("--cpus");
        dockerCommand.add(cpus);
        dockerCommand.add("--memory");
        dockerCommand.add(memory);
        dockerCommand.add("--memory-swap");
        dockerCommand.add(memory);
        dockerCommand.add("--pids-limit");
        dockerCommand.add(pidsLimit);
        dockerCommand.add("--cap-drop");
        dockerCommand.add("ALL");
        dockerCommand.add("--security-opt");
        dockerCommand.add("no-new-privileges");
        dockerCommand.add("--read-only");
        dockerCommand.add("--tmpfs");
        dockerCommand.add("/tmp:rw,noexec,nosuid,size=64m");
        dockerCommand.add("--volume");
        dockerCommand.add(workspace.toAbsolutePath().normalize() + ":" + WORKSPACE_MOUNT + ":rw");
        dockerCommand.add("--workdir");
        dockerCommand.add(WORKSPACE_MOUNT);
        dockerCommand.add(imageFor(language));
        dockerCommand.addAll(command);
        return dockerCommand;
    }

    private String imageFor(String language) {
        String normalized = language.toLowerCase(Locale.ROOT);
        if (normalized.equals("python")) {
            return pythonImage;
        }
        if (normalized.equals("java")) {
            return javaImage;
        }
        throw new IllegalArgumentException("Unsupported language for Docker runner: " + language);
    }

    private void forceRemoveContainer(String containerName) {
        try {
            processRunner.run(List.of("docker", "rm", "-f", containerName), Path.of("."), "", 2_000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
        }
    }
}
