# Reuse-aware feature identification (Slice B) — design

**Date:** 2026-08-25
**Status:** design / approved shape, pre-plan
**Tier:** slice design doc. Owns its own scope.
**Predecessor:** the organism-feature kernel extraction
([design](2026-08-24-organism-feature-kernel-extraction-design.md)); this is that
memo's **Slice B**, redesigned after grounding in the real ingest flow.

## Two problems, kept separate

The catalog holds near-duplicate insect features (e.g. `dark (black) pronotum
contrasting with red elytra` vs `dark/black pronotum contrasting with red
elytra`). There are **two distinct concerns**, and this doc addresses only the first:

- **B (this doc) — stop the *process* minting new near-duplicates.** Modify the
  insect identification flow so it reuses an existing feature when the trait is
  the same, instead of always creating a fresh one.
- **C (separate, later) — fix the *existing* JSON test-data duplicates.** A
  one-time reconciliation of the current `insect-features.json`. Slice C can reuse
  the search component B builds, but it is not in scope here and must not be
  conflated with B.

## Where the duplicates are born (verified)

`POST /insects/identify` → `InsectsController.identify` →
`InsectIdentificationCommand.identify(...)` → `resolveFeatures(...)`
(`InsectIdentificationCommand.java:319-347`). The Anthropic vision model returns a
**free-form** `List<String>` of feature strings (open value space — no controlled
vocabulary). `resolveFeatures` normalizes each (`trim().toLowerCase()`), dedups
**only within the single call** (a `HashMap<String,InsectFeatureId>`), and mints a
fresh `InsectFeature` for every value it hasn't seen *in that call*. **It never
queries existing persisted features.** A `@UniqueValue` DB constraint on
`InsectFeature.value` means *exact* normalized repeats collide, so the live
problem is the **near-variants** the model phrases differently each run. The
problem is live in production whenever `ANTHROPIC_API_KEY` is set (the real
`AnthropicVisionService`); with no key the `NoOpVisionService` throws (no silent
degradation).

## The flow

The generator of the feature text is itself a capable model, so the design puts
the model in the loop rather than auto-merging or hand-tuning a threshold:

1. **Vision turn 1** — the model proposes free-form feature strings (unchanged).
2. **Similarity search** — for each proposed value, the app asks a `FeatureSearch`
   port for similar *existing* insect features (ranked).
3. **Vision turn 2 (clarification)** — only where matches exist, the app returns the
   candidates to the model as a tool result; the model resolves each proposed
   feature to **reuse an existing feature** (by value/id) or **genuinely new**.
4. **Persist** — `resolveFeatures` reuses the existing feature id for resolved
   matches and mints only truly-new features. Assignments are created either way.

This scales (only the relevant candidates per proposed feature reach the model,
never the whole catalog) and carries no false-auto-merge risk (the model judges;
the app never merges on a bare score).

## Component ① — `FeatureSearch` port + in-memory mock

Mirrors the **catalog** strategy (a port with a light in-memory adapter now, a
production Solr/Postgres adapter under `adapters/` later — ADR-024). We build the
**seam and the mock**, never a search engine.

**Per-domain isolation (load-bearing).** Each domain manages its own data; **insect
features and plant features must never share a search.** So unlike `catalog`
(deliberately cross-domain, fan-out over all providers), `FeatureSearch` is a
**generic mechanism instantiated once per domain over that domain's own features**.
Insects wires a `FeatureSearch<InsectFeatureId>` that only ever sees insect
features; plants (when it needs one) wires its own isolated instance.

- **Home:** a small kernel module `kernels/feature-search`
  (`com.naturalist.featuresearch`) holding the port **and** the in-memory mock
  (the mock is light, like `catalog-inmem`; keeping them in one module avoids a
  second kernel module for a single light adapter). The future heavy adapter lives
  under `adapters/` per ADR-024.
- **Port (generic over the feature id type):**
  ```java
  public interface FeatureSearch<ID extends EntityId> {
      List<FeatureMatch<ID>> findSimilar(String value, int limit);
  }
  public record FeatureMatch<ID extends EntityId>(ID id, String value, double score) {}
  ```
  (`limit` bounds the candidates; a default similarity threshold below which
  nothing is returned is a mock config constant, tunable.)
- **Mock (`InMemoryFeatureSearch<ID>`):** normalized **token-set Jaccard** —
  lowercase, split on whitespace/punctuation, drop <2-char fragments, score =
  |A∩B| / |A∪B| over the two token sets — scanning the domain's current features
  and returning the top-`limit` above the threshold, highest first. Deterministic,
  dependency-free, gate-safe (an in-memory scan, never a per-feature repository
  select). **Data source:** the mock reads the domain's current feature set through
  a domain-supplied corpus (a batched "all features" fetch — one call, never N+1);
  exact wiring is a plan detail.
- **Explicitly not now:** a persistent inverted index, edit-distance/embedding
  similarity, or the real Solr/Postgres adapter. Token-set Jaccard in the mock is
  enough to surface the near-variants that dominate the real data; the port lets a
  better implementation land later without touching callers.

## Component ② — multi-turn vision

`VisionService` is single-shot today
(`ToolResult identify(Image, ToolSchema, String systemPrompt)`). The clarification
turn needs a **follow-up exchange**: after turn 1's tool call, the app sends a tool
**result** (the similar candidates) and gets turn 2's tool call.

- Extend `VisionService` to support a continued exchange — a minimal conversation
  handle (e.g. `identify(...)` returns a `VisionExchange` carrying the first
  `ToolResult` plus `respondWithToolResult(json) -> ToolResult`), so the command can
  drive exactly two turns. Exact API shape is a plan detail; the constraint is
  "same model, same tool-use conversation, one follow-up turn."
- Implement the follow-up in `AnthropicVisionService` (append the assistant tool-use
  + the user tool-result to the message list, request again). `NoOpVisionService`
  still throws.
- A **resolution tool schema** for turn 2: given the proposed features (each with
  its similar-existing candidates), the model returns, per proposed feature, either
  `reuse` (the chosen existing value) or `new`.

## Component ③ — `resolveFeatures` change

Insert search + clarification between vision and persistence:

1. Collect the proposed normalized values (turn 1).
2. `findSimilar` per value (batched fetch of existing features underneath).
3. If any proposed value has candidates, run vision turn 2 to get the resolution;
   otherwise skip the turn entirely (no matches → all new, current behavior).
4. Build `newFeatures` / `assignments`: a value resolved to an existing feature
   contributes **no** new `InsectFeature` (its assignment points at the existing
   id); a value resolved new is minted as today. The intra-call `HashMap` stays as
   the innermost guard.

The command already holds an `InsectQuery`; the existing-feature fetch and the
`FeatureSearch` instance are wired through it / the composition root.

## Testing

- Kernel `FeatureSearch` mock: tested through its first consumer (insects) per the
  kernel convention, plus a focused unit test of the Jaccard scoring/threshold edge
  cases (this is genuinely non-orthogonal scoring logic — a warranted direct kernel
  test).
- `InsectIdentificationCommandTest`: a fake `VisionService` scripting both turns —
  cases for (a) no similar candidates (unchanged single-turn behavior), (b) a
  proposed value the model resolves to an existing feature (reused id, no new
  feature row), (c) resolved-new (minted). Assert the persisted
  `newFeatures`/`assignments` reflect the resolution.
- N+1 gate stays green (the existing-feature fetch is one batched call).

## Scope / deferred

- **In scope:** `kernels/feature-search` (port + mock), `VisionService` multi-turn
  + `AnthropicVisionService`, the `resolveFeatures` change, tests. **Insects only.**
- **Deferred:** Slice C (existing-data reconciliation); plants feature search (no
  feature-minting flow yet); the real Solr/Postgres adapter.

## Risks / open items

- **Two model turns per identify** add latency + token cost when matches exist.
  Acceptable; turn 2 is skipped when there are no candidates.
- **Model over-reuse** (resolving to an existing feature that isn't really the same
  trait): softer than deterministic auto-merge, mitigated by a careful turn-2 prompt
  ("reuse only if the same trait"); the model sees the actual candidate text.
- **`VisionExchange` abstraction** is the one non-trivial new seam; the plan pins its
  exact shape against the current `AnthropicVisionService`.
