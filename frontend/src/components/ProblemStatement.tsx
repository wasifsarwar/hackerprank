import type { GenerationMetadata, Problem } from "../types";
import { DraftMetadata } from "./DraftMetadata";

interface ProblemStatementProps {
  generationMetadata?: GenerationMetadata;
  isDraftPreview: boolean;
  problem: Problem;
}

export function ProblemStatement({ generationMetadata, isDraftPreview, problem }: ProblemStatementProps) {
  return (
    <section className="statement">
      <div className="problem-heading">
        <div>
          <p className="eyebrow">
            {problem.difficulty}
            {isDraftPreview ? " Draft" : ""}
          </p>
          <h2>{problem.title}</h2>
        </div>
        <div className="tags">
          {problem.tags.map((tag) => (
            <span key={tag}>{tag}</span>
          ))}
        </div>
      </div>

      {isDraftPreview && generationMetadata ? <DraftMetadata generationMetadata={generationMetadata} /> : null}

      <p>{problem.description}</p>

      <div className="format-grid">
        <section>
          <h3>Input</h3>
          <p>{problem.inputFormat}</p>
        </section>
        <section>
          <h3>Output</h3>
          <p>{problem.outputFormat}</p>
        </section>
      </div>

      <section>
        <h3>Constraints</h3>
        <ul>
          {problem.constraints.map((constraint) => (
            <li key={constraint}>{constraint}</li>
          ))}
        </ul>
      </section>

      {problem.examples.map((example, index) => (
        <section className="example" key={`${example.input}-${index}`}>
          <h3>Example {index + 1}</h3>
          <div className="example-grid">
            <div>
              <span>Input</span>
              <pre>{example.input}</pre>
            </div>
            <div>
              <span>Output</span>
              <pre>{example.output}</pre>
            </div>
          </div>
          <p>{example.explanation}</p>
        </section>
      ))}
    </section>
  );
}
