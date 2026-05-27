package com.hackerprank.problems;

public class TestCase {
    private final String name;
    private final String input;
    private final String expectedOutput;
    private final boolean hidden;

    public TestCase(String name, String input, String expectedOutput, boolean hidden) {
        this.name = name;
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.hidden = hidden;
    }

    public String getName() {
        return name;
    }

    public String getInput() {
        return input;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public boolean isHidden() {
        return hidden;
    }
}
