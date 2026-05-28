package com.hackerprank.submissions;

import com.hackerprank.problems.ProblemRepository;
import com.hackerprank.problems.PublicProblem;

import org.springframework.stereotype.Component;

@Component
class TutorSubmissionContextFactory {
    private final ProblemRepository problemRepository;

    TutorSubmissionContextFactory(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    TutorHintContext create(SubmissionDetail submission) {
        PublicProblem problem = null;
        if (submission.getProblemId() != null && !submission.getProblemId().isBlank()) {
            problem = problemRepository.findById(submission.getProblemId())
                .map(PublicProblem::from)
                .orElse(null);
        }
        return TutorHintContext.from(submission, problem);
    }
}
