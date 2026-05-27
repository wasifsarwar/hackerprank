package com.hackerprank.submissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class DockerSandboxRunnerTests {
    @Test
    void buildsLockedDownPythonDockerCommand() {
        DockerSandboxRunner runner = new DockerSandboxRunner(
            new ProcessRunner(),
            "python:3.12-alpine",
            "eclipse-temurin:21-jdk-alpine",
            "1",
            "256m",
            "128",
            1_000
        );

        List<String> command = runner.buildDockerCommand(
            "python",
            List.of("python3", "main.py"),
            Path.of("/tmp/hackerprank-submission"),
            "hackerprank-test"
        );

        assertEquals("docker", command.get(0));
        assertEquals("run", command.get(1));
        assertTrue(command.contains("--rm"));
        assertTrue(command.contains("--interactive"));
        assertOption(command, "--name", "hackerprank-test");
        assertOption(command, "--network", "none");
        assertOption(command, "--cpus", "1");
        assertOption(command, "--memory", "256m");
        assertOption(command, "--memory-swap", "256m");
        assertOption(command, "--pids-limit", "128");
        assertOption(command, "--cap-drop", "ALL");
        assertOption(command, "--security-opt", "no-new-privileges");
        assertOption(command, "--volume", "/tmp/hackerprank-submission:/workspace:rw");
        assertOption(command, "--workdir", "/workspace");
        assertTrue(command.contains("--read-only"));
        assertTrue(command.contains("python:3.12-alpine"));
        assertEquals(List.of("python3", "main.py"), command.subList(command.size() - 2, command.size()));
    }

    @Test
    void selectsJavaImageForJavaCommands() {
        DockerSandboxRunner runner = new DockerSandboxRunner(
            new ProcessRunner(),
            "python:3.12-alpine",
            "eclipse-temurin:21-jdk-alpine",
            "1",
            "256m",
            "128",
            1_000
        );

        List<String> command = runner.buildDockerCommand(
            "java",
            List.of("javac", "Main.java"),
            Path.of("/tmp/hackerprank-submission"),
            "hackerprank-test"
        );

        assertTrue(command.contains("eclipse-temurin:21-jdk-alpine"));
        assertEquals(List.of("javac", "Main.java"), command.subList(command.size() - 2, command.size()));
    }

    private void assertOption(List<String> command, String option, String value) {
        int optionIndex = command.indexOf(option);
        assertTrue(optionIndex >= 0, "Expected option " + option);
        assertEquals(value, command.get(optionIndex + 1));
    }
}
