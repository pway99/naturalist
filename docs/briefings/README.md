# Briefings

Chat-prompt context documents. **Not plans** — these are uploaded as the
system prompt / first message when asking a chat assistant (Claude.ai,
ChatGPT, etc.) to produce structured output the codebase consumes
directly: JSON catalog entries, life-stage data, design sketches against
the framework conventions.

If you're looking for work-tracking documents, see
[`../plans/README.md`](../plans/README.md).

## What to upload, by task

Keep chat uploads scoped to the task. Uploading more than necessary
burns tokens on rules chat doesn't need and risks bleed-through between
unrelated guidance.

| Task                                                                                                                                | Upload                                                                                                                                                                                                                                |
|-------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Modeling Java** — design or generate an Aggregate, Entity, ValueObject, query / collection / repository contract for a domain api | `framework-kernel.md` + `shared-kernels.md` + one matching `<domain>-domain.md` per domain involved. Multi-domain conversations (e.g. insect chemical defense spanning insects + chemistry) upload one domain briefing per domain. Omit `shared-kernels.md` if the task touches no kernel types beyond framework. |
| **Acquisition** — produce JSON catalog entries the codebase ingests                                                                 | The matching `*-acquisition.md` + `<domain>-domain.md` + the current target JSON file. **Do not** include `framework-kernel.md` — it covers Java conventions chat doesn't need for JSON output.                                     |
| **Domain research** — gather ecological / chemical / botanical context to inform a future task                                      | `<domain>-domain.md` only. Chat brings the wider domain knowledge.                                                                                                                                                                    |

## Framework / structural

| Briefing                                       | Use case                                                                                                                                                  |
|------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| [framework-kernel.md](framework-kernel.md) | Framework APIs, identity model, module layout, complete `Constraints` API, naming conventions. Pair with any domain briefing for **Java modeling tasks**. |
| [shared-kernels.md](shared-kernels.md)     | All shared kernels except framework: field-notes, taxonomy, clades, habitat, biogeography, measurements, catalog, authority. Pair with `framework-kernel.md`. |

## Domain

| Briefing                               | Use case                                                                                                                              |
|----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| [plants-domain.md](plants-domain.md)   | Narrative, ecological, and historical context for the plants domain. Pair with `domains/plants/CLAUDE.md`.                            |
| [chemistry-domain.md](chemistry-domain.md) | Domain vocabulary and current shape of the `chemistry-api` module. Pair with `framework-kernel.md`.                             |
| [insects-domain.md](insects-domain.md) | Domain vocabulary and current shape of `insects-api` (species, image, life-stage sub-context). Pair with `domains/insects/CLAUDE.md`. |
| [library-domain.md](library-domain.md) | Domain vocabulary and current shape of `library-api` (concept, citation, citation-association sub-contexts). Pair with `framework-kernel.md`. |

## Acquisition tasks (chat assistant produces JSON)

| Briefing                                                           | Output appended to                                                                                       |
|--------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| [compound-acquisition.md](compound-acquisition.md)                 | `domains/chemistry/chemistry-repository-test/.../chemistry/compound/compounds.json`                      |
| [product-acquisition.md](product-acquisition.md)                   | `.../chemistry/product/products-base.json` (+ compounds where new)                                       |
| [insect-lifestage-acquisition.md](insect-lifestage-acquisition.md) | `domains/insects/.../insect-species.json` (populating `egg`/`larva`/`pupa`/`adult` for existing entries) |

## Convention

- New briefing? Drop it here and add a row above.
- A briefing is *only* for prompting an external chat assistant or for
  loading into a fresh chat session — don't put architectural decisions,
  task lists, or implementation notes here.
- Briefings should pair with a `CLAUDE.md`, never replace it.
