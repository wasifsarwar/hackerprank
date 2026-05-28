# Generated Problem Fixtures

These fixtures exercise the generated-problem contract without calling an AI provider.

- `valid-*.json` fixtures should pass shape checks and Python/Java reference-solution validation.
- `invalid-*.json` fixtures should fail for the contract reason declared in `expectedFailureMessage`.

When changing prompt versions, validation rules, or generated-problem mapping, add a fixture that captures the behavior before tuning the implementation.
