package com.hackerprank.submissions;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {
    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping
    public List<SubmissionSummary> list(
        @RequestParam(required = false) String problemId,
        @RequestParam(required = false) Integer limit
    ) {
        return submissionService.findRecent(problemId, limit);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionDetail> get(@PathVariable String id) {
        return submissionService.findById(id)
            .map(submission -> ResponseEntity.ok(submission))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/run")
    public SubmissionResult run(@RequestBody SubmissionRequest request) {
        return submissionService.run(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}
