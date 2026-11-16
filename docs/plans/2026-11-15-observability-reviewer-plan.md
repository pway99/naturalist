# Observability Reviewer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A read-only, Kaname-backed reviewer agent that checks a change against the observability
standards; the writing session runs it before "done" (A), and Pat runs it on any PR or branch (B).

**Architecture:** One project agent (`.claude/agents/observability-reviewer.md`) holds the review
procedure. A thin project skill (`/observability-review`) resolves a PR/branch to a checkout and
dispatches it. One CLAUDE.md line makes the writing session dispatch it before declaring a task done.

**Tech Stack:** Claude Code project agents and skills (Markdown + YAML frontmatter), git, `gh`,
Kaname Engine MCP tools.

**Spec:** [`docs/plans/2026-11-15-observability-reviewer-design.md`](2026-11-15-observability-reviewer-design.md);
rubric [`docs/observability-standards.md`](../observability-standards.md).

## Global Constraints

- The reviewer never edits: its tools exclude `Edit`, `Write`, `NotebookEdit` and every Kaname write
  (`apply_change`, `java_*_change`/`_add`/`_remove`, `session_*`).
- The standards doc is the reviewer's only authority; it does not cite ADR-017, kernels/CLAUDE.md or memory.
- Report row = rule ID, `file:line`, what it saw, the fix; most severe first (J*, then B*, C*, M*);
  *uncertain* when the engine answered `cannotSee` or the reviewer could not read the context.
- M13 and every Known-gaps item are reported only on touched lines.
- Nothing is posted to GitHub unless Pat asks. No `mvn install` in a review worktree.
- Commits: stage only, print the `git commit` command; Pat commits (memory: git workflow).
- A new project agent is loaded at session start: verification of Tasks 1–3 runs in a **new session**.

## Review Focus

1. **The target checkout has no envelope** (a fresh review worktree) → the reviewer builds one for
   that root; it never traces against main's envelope. Pinned in Task 1, fixture run (worktree is new).
2. **The Kaname server is down** → the report says so in its first line and marks every finding
   *uncertain*; it does not present grep guesses as engine facts. Pinned in Task 1, step 6.
3. **A change removes an observation** (deletes a `noticeError` but keeps the throw) → flagged,
   because the reviewer judges each changed method in its post-change form. Pinned in the fixture.
4. **Only tests or docs changed** → "No findings: no production Java changed." Production = files
   under `src/main/java`; `src/test/**` is not reviewed. Pinned in Task 1, step 5.
5. **An empty diff** (branch already merged) → "No findings: nothing changed against <base>."
   Pinned in Task 2, step 4.

---

### Task 1: Reviewer agent

**Files:**
- Create: `.claude/agents/observability-reviewer.md`
- Fixture (throwaway, not committed): branch `review-fixture/observability` in worktree
  `temp/review/review-fixture-observability`

**Interfaces:**
- Produces: agent name `observability-reviewer`. Input (in the dispatch prompt): `checkout: <abs path>`,
  optional `base: <ref>` (default `$(git merge-base HEAD main)`, plus uncommitted changes).
  Output: the report, first line exactly one of `Findings: <n> (<k> uncertain)`,
  `No findings: <reason>`, or `Engine unavailable: <reason>` followed by findings marked uncertain.

- [ ] **Step 1: Build the fixture (the failing test).** From main, `git worktree add
  temp/review/review-fixture-observability -b review-fixture/observability`. In that worktree, make
  one uncommitted-then-committed change per expected finding, each in production code under
  `src/main/java`:
  - F1 (J1): a new `throw new IllegalStateException(...)` with no observation.
  - F2 (J2): a refusal (`throw new <Domain>Exception`) after an `if` decision, decision not observed.
  - F3 (J3): a `catch` that rethrows wrapped with `noticeError(..., Handling.RETHROWN)`.
  - F4 (Review Focus 3): delete an existing `noticeError` line, keep its throw.
  - F5 (M13, touched line): a new `MethodObserver m = observer.forMethod(...)` used twice.
  - C0 (control, must NOT be flagged): a throw preceded by a correct `methodObserver.noticeError(..., Handling.WRAPPED)`.
  - T0 (control): an unobserved throw added under `src/test/java` — must NOT be flagged.
  Record each `file:line` in `temp/review/fixture-expected.md`.

- [ ] **Step 2: Write `.claude/agents/observability-reviewer.md`.** Frontmatter: `name:
  observability-reviewer`; `description:` says when to use it (before declaring a Java task done; on
  request for a PR/branch) and that it is read-only; `model: opus`; `tools:` `Read, Grep, Glob, Bash`
  plus `mcp__plugin_kaname-engine_kaname-engine__` + each of `java_failure_handling_census,
  jvm_find_method, jvm_find_type, java_class_source, java_file_outline, envelope_staleness,
  envelope_build, reindex, list_projects`. Body, in this order:
  1. Bash is for `git` and `gh` reads only; never edit, stage, commit or install.
  2. Read `<checkout>/docs/observability-standards.md` in full first; it is the only authority.
  3. Engine readiness: `list_projects` → envelope for `<checkout>`? none → `envelope_build
     {projectRoot: <checkout>}`; else `envelope_staleness` → stale → `reindex`/rebuild. Any engine
     call failing → `Engine unavailable:` report.
  4. Scope: `git -C <checkout> diff <base>` (+ working tree); keep files under `src/main/java`;
     collect each changed method (via `java_file_outline` line ranges) in its post-change form.
  5. Per changed method, apply every rule using the engine calls the standards doc's
     "The reviewer uses the Kaname Engine" table maps to it; read handler bodies with
     `java_class_source member` because the census cannot see Observer calls.
  6. Report format and ordering as in Global Constraints; touched-lines-only for M13/Known gaps.

- [ ] **Step 3: Verify the fixture fails as expected (new session).** In a new Claude session at the
  repo root, dispatch `observability-reviewer` with `checkout: <abs>/temp/review/review-fixture-observability`.
  Expected: an envelope build for that worktree is visible in the run; findings for F1 (J1), F2 (J2),
  F3 (J3, `RETHROWN` should be `WRAPPED`), F4 (J1), F5 (M13) at the recorded lines; no finding on C0 or T0.

- [ ] **Step 4: Iterate the agent body** until Step 3's expected matches exactly (wording changes only;
  never relax a rule in the standards doc to make the fixture pass).

- [ ] **Step 5: Tests-only diff.** In the fixture worktree, `git stash` / revert all but T0; dispatch.
  Expected first line: `No findings: no production Java changed.`

- [ ] **Step 6: Engine down.** Stop the Kaname server, dispatch on the fixture. Expected first line
  begins `Engine unavailable:`; every row marked *uncertain*. Restart the server.

- [ ] **Step 7: Stage.** `git add .claude/agents/observability-reviewer.md`; print
  `git commit -m "feat(review): observability reviewer agent"` (with the Co-Authored-By footer).

### Task 2: `/observability-review` skill (B)

**Files:**
- Create: `.claude/skills/observability-review/SKILL.md`

**Interfaces:**
- Consumes: agent `observability-reviewer` and its input/output contract (Task 1).
- Produces: `/observability-review [<PR number> | <branch>]`.

- [ ] **Step 1: Write the skill.** Frontmatter `name: observability-review`, `description:` with
  triggers ("review PR 3 for observability", "observability review of <branch>"). Body:
  1. No argument → `checkout` = current repo root, base = merge-base with `main`.
  2. PR number → `gh pr view <n> --json headRefName,baseRefName`; branch → that name.
  3. `git worktree list` → reuse a worktree on that branch; else `git fetch origin <branch>` and
     `git worktree add temp/review/<branch-with-slashes-as-dashes> <branch>`; base = merge-base with
     the PR's base ref (default `main`).
  4. Dispatch `observability-reviewer` with `checkout` and `base`; print its report verbatim to Pat.
  5. Never post to GitHub; never delete the review worktree.

- [ ] **Step 2: Verify on PR #3 (new session).** `/observability-review 3`. Expected: reuses
  `.claude/worktrees/observe-every-throw-8d0d18` (its branch is checked out there), report produced.

- [ ] **Step 3: Verify on a branch with no worktree.** `/observability-review review-fixture/observability`
  after removing that worktree. Expected: worktree recreated under `temp/review/`, envelope built,
  Task 1 Step 3 findings again.

- [ ] **Step 4: Empty diff.** `/observability-review main`. Expected first line:
  `No findings: nothing changed against <base>.`

- [ ] **Step 5: Stage.** `git add .claude/skills/observability-review/SKILL.md`; print the commit
  command `feat(review): /observability-review skill`.

### Task 3: Wire into the writing session (A) and close the doc loop

**Files:**
- Modify: `CLAUDE.md` — "Completeness verification" section and the "Area-specific conventions" list
- Modify: `docs/observability-standards.md` — "Open questions" → move reviewer setup to "Decided"
- Commit together with: `docs/observability-standards.md` (first commit of the draft),
  both `docs/plans/2026-11-15-observability-reviewer-*.md`

- [ ] **Step 1: CLAUDE.md.** Add to "Area-specific conventions":
  `- [docs/observability-standards.md](docs/observability-standards.md) — observability rules; wins over older guidance`.
  Add to "Completeness verification", after the rewrite gate paragraph: before declaring complete any
  task that changed `src/main/java`, dispatch the `observability-reviewer` agent on the current
  checkout, fix every finding, re-dispatch until it reports `No findings`, and tell Pat only the
  findings left (uncertain, or needing his decision).

- [ ] **Step 2: Standards doc.** Replace open question 1 with a "Decided (Pat, 2026-11-15)" entry:
  reviewer = read-only project agent + `/observability-review` skill; writer dispatches it before done;
  Stop hook deferred. Link the design doc.

- [ ] **Step 3: Verify A end to end (new session).** Ask a new session to add an unobserved throw to
  a production class in a scratch worktree and "finish the task". Expected: it dispatches the
  reviewer unprompted, adds the observation, re-dispatches, ends on `No findings`. Discard the worktree.

- [ ] **Step 4: Clean up the fixture.** `git worktree remove temp/review/review-fixture-observability`;
  `git branch -D review-fixture/observability`; delete `temp/review/fixture-expected.md`.

- [ ] **Step 5: Stage and print the commit.** `git add CLAUDE.md docs/observability-standards.md
  docs/plans/2026-11-15-observability-reviewer-design.md docs/plans/2026-11-15-observability-reviewer-plan.md`;
  print `git commit -m "docs: observability standards + reviewer wiring"`.
