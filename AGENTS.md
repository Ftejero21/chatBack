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

# Mandatory validation rules

- Always apply full defensive validation by default, even if the user only asked for functional changes.
- Never trust controller validation alone. Critical validation must also exist in the service layer.
- Add or reuse request DTOs instead of using Map<String, String> or raw untyped payloads for non-trivial inputs.
- Use Bean Validation annotations (`@Valid`, `@Validated`, `@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Pattern`, `@Min`, `@Max`, etc.) wherever applicable.
- Add explicit bounds for length, size, count, range, pagination, filters, and payload volume.
- Validate enums, ids, ownership, state transitions, and cross-field consistency.
- For create/update flows, validate both field format and business invariants.
- Reject unexpected combinations of fields. Do not silently ignore dangerous or inconsistent inputs.
- For text fields, enforce maximum lengths and safe normalization.
- For collections, reject null entries, duplicates, empty invalid payloads, and oversized lists.
- For uploads and binary content, validate MIME, extension, size, ownership, and authorization on the target resource.
- For URLs, file paths, and external references, validate format, allowed origin, and access rules.
- If a validation is missing nearby, add it unless doing so would clearly break an intentional contract.

# Authorization and access control rules

- Enforce authentication, authorization, and least privilege in every sensitive service method.
- Never trust userId, requesterId, role, chatId, messageId, or ownership-related values coming from the client without server-side verification.
- Always validate that the authenticated user has access to the target resource.
- Validate ownership and membership for chats, messages, groups, files, calls, and admin actions.
- Prefer deriving actor identity from the security context instead of request payloads.
- Sensitive crypto, backup, token, password, and account-recovery operations must default to self-only unless explicitly required otherwise.
- Flag and block any change that weakens authentication, authorization, validation, encryption, logging, secret handling, or access control.

# Security rules

- Treat all external input as untrusted.
- Do not expose secrets, tokens, credentials, personal data, internal paths, or stack traces.
- Avoid user enumeration in login, recovery, signup, or public workflows.
- Prefer neutral public error messages when existence checks could leak account state.
- Add rate limiting or anti-abuse checks to public, auth, recovery, upload, complaint, invite, scheduling, and admin-sensitive flows when relevant.
- Prefer safe error handling over silent failures.
- Prefer production-safe implementations over convenience.
- If a requested change is insecure, implement the safe version and briefly state the risk.

# Controller rules

- Controllers should use typed request DTOs and `@Valid` by default.
- Do not accept raw maps for sensitive operations when a DTO can be defined.
- Keep controllers thin. Put business validation and authorization in services.
- Use consistent response and exception handling patterns.

# Service rules

- Services must enforce business validation, authorization, and invariant checks even if the controller already validates.
- Never assume a validated controller means the service is safe.
- Validate resource existence, ownership, allowed state, and operation legality before mutating data.
- For updates, validate that the new state is allowed from the current state.
- For security-sensitive flows, prefer fail-closed behavior.

# Implementation rules

- Keep methods small and focused.
- Prefer explicit logic over ambiguous shortcuts.
- For database changes, prefer safe and reversible migrations.
- For concurrent or async flows, avoid race conditions and unsafe shared state.
- Follow existing repository conventions before inventing new patterns.
- Reuse existing validation helpers/utilities if they already exist and are adequate.
- If repeated validation is needed in multiple services, extract it only when it clearly improves consistency.

# Verification

- Before finishing, verify compile-time correctness.
- Perform a static security review of the touched code before finalizing.
- Check for missing validation, missing authorization, unsafe null handling, broken invariants, and obvious abuse paths.
- Do not run unit tests.
- Do not execute test suites unless explicitly requested.
- If validation is needed, prefer static review and compile-time checks only.
- If checks cannot be run, state the blocker briefly.
- Report only: changed files, blocking risk, next action.

# Done criteria

- Change is implemented.
- Required validation is present in DTO/controller/service where appropriate.
- Authorization and ownership checks are present for sensitive operations.
- No unrelated files touched.
- No obvious security regression introduced.
- Code follows repository conventions.
- Compile-time checks pass, or the exact blocker is stated.