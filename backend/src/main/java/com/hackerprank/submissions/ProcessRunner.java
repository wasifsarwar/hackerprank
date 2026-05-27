package com.hackerprank.submissions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

@Component
class ProcessRunner {
    ProcessResult run(List<String> command, Path workingDirectory, String input, long timeoutMs)
        throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());

        long startedAt = System.nanoTime();
        Process process = builder.start();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<String> stdout = executor.submit(readStream(process.getInputStream()));
        Future<String> stderr = executor.submit(readStream(process.getErrorStream()));

        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(input.getBytes(StandardCharsets.UTF_8));
        }

        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
        }

        int exitCode = finished ? process.exitValue() : -1;
        String stdoutText = getFuture(stdout);
        String stderrText = getFuture(stderr);
        long runtimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        executor.shutdownNow();

        return new ProcessResult(exitCode, stdoutText, stderrText, !finished, runtimeMs);
    }

    private Callable<String> readStream(InputStream inputStream) {
        return () -> {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int count;
            while ((count = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, count);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        };
    }

    private String getFuture(Future<String> future) {
        try {
            return future.get(100, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            return "";
        }
    }
}
