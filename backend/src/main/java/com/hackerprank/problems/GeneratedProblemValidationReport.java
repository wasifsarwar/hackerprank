package com.hackerprank.problems;

record GeneratedProblemValidationReport(String status, String errors, String summary) {
    static GeneratedProblemValidationReport validated(String summary) {
        return new GeneratedProblemValidationReport("VALIDATED", "", summary);
    }
}
