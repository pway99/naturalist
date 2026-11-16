---
name: observability-review
description: Review a PR, a branch, the current checkout, or a path of existing code (a file, package or module) against docs/observability-standards.md by dispatching the read-only observability-reviewer agent (Kaname Engine backed). Use when Pat asks to "review PR 3 for observability", "run an observability review of <branch>", "review kernels/catalog-inmem for observability", or invokes "/observability-review [PR# | branch | path...]".
---

# Observability review (on demand)

Resolve the target to a checkout, dispatch the `observability-reviewer` agent on it, and show Pat
its report. You do not review yourself and you never fix anything here.

## 1. Resolve the target

From the repo root (`git rev-parse --show-toplevel` of the main checkout):

- **No argument** → `checkout` = the current checkout's root; `base` = `git merge-base HEAD main`.
- **One or more paths that exist in the main checkout** (a file or directory, e.g.
  `kernels/catalog-inmem` or `.../InsectsController.java`) → **path mode**: `checkout` = the main
  checkout's root, and dispatch with `paths:` instead of `base:` (skip the worktree steps below).
  If an argument could be both a path and a branch, it is a path.
- **A number** → a PR: `gh pr view <n> --json headRefName,baseRefName,state`. Branch =
  `headRefName`; base branch = `baseRefName` (usually `main`).
- **Anything else** → a branch name; base branch = `main`.

For a PR or branch:

1. `git worktree list --porcelain` — if a worktree already has that branch checked out, use it as
   `checkout` (do not touch its working tree).
2. Otherwise `git fetch origin <branch>` (skip if it is local-only) and
   `git worktree add temp/review/<branch with / replaced by -> <branch>`; that path is `checkout`.
   Never run `mvn install` there — it would share `~/.m2` with the main checkout.
3. `base` = `git -C <checkout> merge-base HEAD <base branch>` (use `origin/<base branch>` if the
   local one is behind).

## 2. Dispatch

Dispatch the `observability-reviewer` agent with:

```
checkout: <absolute path>
base: <sha>
```

or, in path mode:

```
checkout: <absolute path of the main checkout>
paths: <path> [<path> ...]
```

## 3. Report

Print the agent's report to Pat verbatim, preceded by one line naming the target, the checkout
path and the base. Do not post to GitHub unless Pat asks. Leave the review worktree in place
(it is under `temp/`); say its path so Pat can remove it with `git worktree remove`.
