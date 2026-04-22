# ADR-009: Accuracy, Precision, and Domain Authority

**Status:** Accepted
**Full rationale:** [rationale/ADR-009-accuracy-precision-and-domain-authority.md](rationale/ADR-009-accuracy-precision-and-domain-authority.md)

## Decision

### Confidence is domain-dependent

- **High confidence** — framework architecture, identity model, repository contracts,
  test infrastructure. Closed decisions, changed only by first-principles structural analysis.
- **Moderate confidence** — agronomic rules from extension publications and soil science
  texts. Consensus values with regional variation. Cite sources; local conditions may shift optima.
- **Low confidence** — emerging ecological models, microbial dynamics, complex nutrient
  interactions. Encoded as provisional, marked `@Incubating`, evolve with observation.

### Every scientific claim needs a reference

Domain constants, optimal ranges, safety thresholds, ecological classifications, and
agronomic rules must cite a published source:
- Peer-reviewed paper or textbook (fundamental science)
- University extension publication (regional agronomic practice)
- Manufacturer's technical data sheet (product-specific data)
- Field guide or identification manual (taxonomy, species behaviour)

A value without a reference cannot be challenged and has no place in a naturalist's toolkit.

### The reference library is a first-class domain concept

Planned module, not bibliography appendix. Purposes: traceability, education, correction
when research supersedes old values.

### Claude's role: explanation, not authority

Durrell Description pattern is explanation. The reference library provides authority.
Goal: informed naturalist who evaluates models, not dependent user trusting the app.

## Consequences

- Framework ADRs treated as high-confidence closed decisions; science-domain models provisional
- Every scientific constant/threshold/range/classification in code cites a source
- Reference library is a planned first-class domain module
- `@Incubating` used liberally in science modules
- Durrell multi-level explanations remain; explanation ≠ authority
- Regional values acknowledge local adjustment
- Naturalist is final authority on model fit to observations
