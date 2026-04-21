# AGENTS.md

# Response style

Use Caveman mode by default in this repository at level `Ultra Max`.

- Keep answers terse and technical.
- Minimize output tokens aggressively.
- Use the fewest words possible without losing correctness.
- Drop filler, pleasantries, hedging, and recap.
- Do not restate the user's request.
- Give only the result, blocking risk, or next action.
- Prefer 1-2 short lines. Use 3 only if needed.
- Use bullets only when strictly necessary.
- For simple confirmations, answer with one short sentence.
- For task completion, report only the smallest useful summary.
- Only use normal clear language when writing prompts.

# Backend defaults

- Prioritize security and backend best practices by default.
- Prefer secure, maintainable, production-safe solutions over shortcuts.
- Preserve existing architecture and naming unless there is a strong reason to change them.
- Prefer minimal diffs. Do not refactor unrelated code.
- Reuse existing services, utils, DTOs, mappers, constants, and patterns before introducing new abstractions.
- Keep entities, DTOs, repositories, services, implementations, mappers, and controllers separated.
- Avoid duplicate logic. Extract shared logic only when it clearly reduces repetition.
- Do not introduce new dependencies unless necessary and justified.
- Maintain backwards compatibility for existing endpoints unless explicitly asked to break it.
- Do not change API contracts, database schema, or public DTOs unless required.

# Security rules

- Validate all inputs strictly.
- Treat all external input as untrusted.
- Enforce authentication, authorization, and least privilege.
- Do not expose secrets, tokens, credentials, personal data, or stack traces.
- Flag any change that weakens authentication, authorization, validation, encryption, logging, secret handling, or access control.
- Prefer safe error handling over silent failures.
- Prefer production-safe implementations over convenience.

# Implementation rules

- Keep methods small and focused.
- Prefer explicit logic over ambiguous shortcuts.
- For database changes, prefer safe and reversible migrations.
- For concurrent or async flows, avoid race conditions and unsafe shared state.
- Follow existing repository conventions before inventing new patterns.

# Verification

- Before finishing, verify compile-time correctness.
- Do not run unit tests.
- Do not execute test suites unless explicitly requested.
- If validation is needed, prefer static review and compile-time checks only.
- If checks cannot be run, state the blocker briefly.
- Report only: changed files, blocking risk, next action.

# Done criteria

- Change is implemented.
- No unrelated files touched.
- No obvious security regression introduced.
- Code follows repository conventions.
- Compile-time checks pass, or the exact blocker is stated.