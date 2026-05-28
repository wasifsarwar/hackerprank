package com.hackerprank.problems;

class OpenAiProblemGenerationException extends RuntimeException {
    OpenAiProblemGenerationException(String message) {
        super(message);
    }

    OpenAiProblemGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
