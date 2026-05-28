package com.hackerprank.submissions;

class AnthropicTutorException extends RuntimeException {
    AnthropicTutorException(String message) {
        super(message);
    }

    AnthropicTutorException(String message, Throwable cause) {
        super(message, cause);
    }
}
