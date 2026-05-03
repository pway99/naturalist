# Briefings

Chat-prompt context documents. **Not plans** — these are uploaded as the
system prompt / first message when asking a chat assistant (Claude.ai,
ChatGPT, etc.) to produce structured output the codebase consumes
directly: JSON catalog entries, life-stage data, design sketches against
the framework conventions.

If you're looking for work-tracking documents, see
[`../plans/README.md`](../plans/README.md).

## Framework / structural

| Briefing                     | Use case                                                                                                                 |
|------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| [framework.md](framework.md) | Framework APIs, identity model, module layout, naming conventions, observability surface. Pair with any domain briefing. |

## Domain

| Briefing                             | Use case                                                                                                   |
|--------------------------------------|------------------------------------------------------------------------------------------------------------|
| [plants-domain.md](plants-domain.md) | Narrative, ecological, and historical context for the plants domain. Pair with `domains/plants/CLAUDE.md`. |
| [chemistry-api.md](chemistry-api.md) | Domain vocabulary and current shape of the `chemistry-api` module. Pair with `framework.md`.               |

## Acquisition tasks (chat assistant produces JSON)

| Briefing                                             | Output appended to                                                                                       |
|------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| [compound-acquisition.md](compound-acquisition.md)   | `domains/chemistry/chemistry-repository-test/.../chemistry/compound/compounds.json`                      |
| [product-acquisition.md](product-acquisition.md)     | `.../chemistry/product/products-base.json` (+ compounds where new)                                       |
| [insect-lifestage-task.md](insect-lifestage-task.md) | `domains/insects/.../insect-species.json` (populating `egg`/`larva`/`pupa`/`adult` for existing entries) |

## Scratchpads

Short-lived prompt material that may grow into a proper briefing or be
discarded. Treat as draft.

| File                                         | Notes                                                                           |
|----------------------------------------------|---------------------------------------------------------------------------------|
| [prompt-scratchpad.md](prompt-scratchpad.md) | Loose chat-prompt fragments (Metric/MetricRegistry follow-ups, refactor TODOs). |

## Convention

- New briefing? Drop it here and add a row above.
- A briefing is *only* for prompting an external chat assistant or for
  loading into a fresh chat session — don't put architectural decisions,
  task lists, or implementation notes here.
- Briefings should pair with a `CLAUDE.md`, never replace it.
