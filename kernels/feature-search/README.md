# feature-search

A search seam for reuse-aware feature identification: given a candidate feature
value, find the existing features whose text is close to it. It exists so a
domain can reuse an already-recorded feature instead of minting a near-duplicate
when identifying an organism. The kernel carries no domain knowledge.

---

## What it provides

**`FeatureSearch<ID>` — the port.** `findSimilar(String value, int limit)`
returns the closest existing features, best first, each a `FeatureMatch<ID>`
carrying the feature's id, its value, and a similarity score. It is parameterised
by `EntityId` type, and each domain wires its own instance over its own features
— insect features and plant features never share a search.

**`FeatureCorpus<ID>` — the domain's supply side.** A functional interface a
domain implements by streaming its current features as `Indexed<ID>` records
(id and value). The stream is loaded in one batch, never a per-feature select.

**`InMemoryFeatureSearch<ID>` — the default implementation.** A dependency-free
matcher that tokenises and normalises text and ranks candidates by token-set
Jaccard overlap, returning those above a tunable threshold. It loads the corpus
once and scans in memory, so it triggers no per-feature query fan-out. It backs
tests and the development runtime; a production Solr- or Postgres-backed adapter
can implement the same `FeatureSearch` port later without disturbing consumers.

---

## Learn more

- [Backyard Naturalist README](../../README.md) — the module map placing
  feature-search among the kernels.
