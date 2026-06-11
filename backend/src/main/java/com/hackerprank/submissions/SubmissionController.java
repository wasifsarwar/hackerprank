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
    private final TutorHintService tutorHintService;
    private final TutorChatService tutorChatService;

    public SubmissionController(
        SubmissionService submissionService,
        TutorHintService tutorHintService,
        TutorChatService tutorChatService
    ) {
        this.submissionService = submissionService;
        this.tutorHintService = tutorHintService;
        this.tutorChatService = tutorChatService;
    }

    @GetMapping
    public List<SubmissionSummary> list(
        @RequestParam(required = false) String problemId,
        @RequestParam(required = false) Integer limit
    ) {
        return submissionService.findRecent(problemId, limit);
    }

    @GetMapping("/solved")
    public List<String> solvedProblemIds() {
        return submissionService.findSolvedProblemIds();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionDetail> get(@PathVariable String id) {
        return submissionService.findById(id)
            .map(submission -> ResponseEntity.ok(submission))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/hint")
    public ResponseEntity<TutorHintResponse> hint(@PathVariable String id) {
        return submissionService.findById(id)
            .map(submission -> ResponseEntity.ok(tutorHintService.createHint(submission)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/tutor/messages")
    public ResponseEntity<List<TutorMessage>> tutorMessages(@PathVariable String id) {
        return submissionService.findById(id)
            .map(submission -> ResponseEntity.ok(tutorChatService.findMessages(submission.getId())))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/tutor/messages")
    public ResponseEntity<TutorChatResponse> tutorMessage(
        @PathVariable String id,
        @RequestBody TutorMessageRequest request
    ) {
        return submissionService.findById(id)
            .map(submission -> ResponseEntity.ok(tutorChatService.createReply(submission, request)))
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
