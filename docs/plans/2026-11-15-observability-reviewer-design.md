# Observability reviewer — design

Status: draft, 2026-11-15. Rubric: [`docs/observability-standards.md`](../observability-standards.md).

## Intent

Cut Pat's review time. Observability findings (an unnoticed throw, an unobserved decision, the
wrong `Handling`) need judgment, so no Rewrite recipe can catch them. A reviewer applies the
standards with the Kaname Engine and reports; the session that wrote the code fixes them before
Pat ever reads the change.

## Goals, non-goals, anti-goals

- **Goal (A, the default):** every development task is reviewed before the writing session says
  "done". Pat sees only what the session could not settle.
- **Goal (B, on demand):** Pat can review any PR or branch and get a report to read beside the diff.
- **Non-goal:** general code review (bugs, style, design); `/code-review` covers that.
  Not a CI job: the engine runs only on Pat's Mac.
- **Anti-goals:** a reviewer that edits code; a reviewer that floods the report with old-code
  findings (outside the changed methods it reports only what it met, separately, as existing-code
  concerns); a reviewer that cites
  anything but the standards doc as authority.

## Approaches

1. **Skill only.** The writer loads the rubric and reviews its own diff. Rejected: self-review
   is anchored on the author's belief that the code is right, which is exactly how throws go
   unnoticed; it also fills the writer's context.
2. **Reviewer agent + thin on-demand skill.** **Chosen (Pat, 2026-11-15).** A fresh, read-only
   reviewer is independent of the writer; A and B share one rubric and one reviewer.
3. **Approach 2 + a Stop hook forcing the review.** Deferred: the hook fires on every turn
   (questions, docs-only edits) and telling code changes apart in a shell script is fragile. Add it
   only if sessions skip the CLAUDE.md step.

## Design

### `.claude/agents/observability-reviewer.md`

- Read-only: `Read`, `Bash` (git diff/log only), and the Kaname read tools
  (`java_failure_handling_census`, `jvm_find_method`, `jvm_find_type`, `java_class_source`,
  `java_file_outline`, `envelope_staleness`, `envelope_build`, `reindex`). No `Edit`/`Write`.
- Input: a checkout path and a base (default: merge-base with `main`, plus uncommitted changes).
- Steps:
  1. Read `docs/observability-standards.md` — the only authority.
  2. Ensure the checkout's envelope is fresh (`envelope_staleness`; build or reindex if not), so
     the census traces the code under review, not main's.
  3. List changed Java files and changed line ranges from the diff.
  4. For each changed method: find every `throw`, catch, refusal and enum mapping in the touched
     lines and apply the rules through the engine calls the standards doc maps to each rule.
  5. Report.
- Report: one row per finding — rule ID, `file:line`, what it saw, the fix — most severe first
  (J1/J2 before mechanical). Marked *uncertain* when the engine answered `cannotSee` or the call
  depended on context the reviewer could not read. Ends with "no findings" explicitly when clean.

### `.claude/skills/observability-review/SKILL.md` (B)

- `/observability-review [PR# | branch]`; no argument = the current checkout.
- Resolves the target to a checkout: an existing worktree on that branch if there is one, else a
  fresh worktree under `temp/review/<branch>` (no `mvn install` there, so the shared `~/.m2` is
  safe). Dispatches the reviewer with that path and base, and prints the report to Pat.
- Never posts to GitHub unless Pat asks.

### `CLAUDE.md` — Completeness verification (A)

One added line: before declaring a task that touched Java complete, dispatch the
`observability-reviewer` agent on the current checkout, fix its findings, re-run until clean, and
report to Pat only the findings left (uncertain ones, or ones that need his call).

## Left to the builder

The agent's exact prompt wording and report layout; the Bash allow-list for git; whether the
skill cleans up the review worktree afterwards (default: keep it, it's under `temp/`).
