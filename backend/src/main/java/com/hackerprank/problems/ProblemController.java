package com.hackerprank.problems;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {
    private final ProblemRepository repository;

    public ProblemController(ProblemRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProblemSummary> listProblems() {
        return repository.findAll().stream()
            .map(ProblemSummary::from)
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicProblem> getProblem(@PathVariable String id) {
        return repository.findById(id)
            .map(problem -> ResponseEntity.ok(PublicProblem.from(problem)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
