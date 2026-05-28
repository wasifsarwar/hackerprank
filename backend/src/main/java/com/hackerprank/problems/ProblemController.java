package com.hackerprank.problems;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {
    private final ProblemRepository repository;
    private final ProblemGeneratorService generatorService;

    public ProblemController(ProblemRepository repository, ProblemGeneratorService generatorService) {
        this.repository = repository;
        this.generatorService = generatorService;
    }

    @GetMapping
    public List<ProblemSummary> listProblems() {
        return repository.findAllSummaries();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicProblem> getProblem(@PathVariable String id) {
        return repository.findById(id)
            .map(problem -> ResponseEntity.ok(PublicProblem.from(problem)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/generate")
    public PublicProblem generateProblem(@RequestBody(required = false) GenerateProblemRequest request) {
        return generatorService.generate(request);
    }

    @PostMapping("/drafts")
    public PublicProblemDraft createDraft(@RequestBody(required = false) GenerateProblemRequest request) {
        return generatorService.createDraft(request);
    }

    @GetMapping("/drafts/{id}")
    public ResponseEntity<PublicProblemDraft> getDraft(@PathVariable String id) {
        return generatorService.findDraft(id)
            .map(draft -> ResponseEntity.ok(draft))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/drafts/{id}/publish")
    public ResponseEntity<PublicProblem> publishDraft(@PathVariable String id) {
        return generatorService.publishDraft(id)
            .map(problem -> ResponseEntity.ok(problem))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/drafts/{id}")
    public ResponseEntity<Void> deleteDraft(@PathVariable String id) {
        if (generatorService.deleteDraft(id)) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleGenerationFailure(IllegalStateException exception) {
        return ResponseEntity.internalServerError().body(exception.getMessage());
    }
}
