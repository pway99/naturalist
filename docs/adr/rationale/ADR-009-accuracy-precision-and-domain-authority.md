# ADR-009: Accuracy, Precision, and Domain Authority

**Status:** Accepted

## Context

The amateur Naturalist spans two fundamentally different kinds of domain knowledge, and
the confidence with which design decisions can be made differs sharply between them.

### Framework and application architecture

The application framework — entity identity, repository contracts, module governance,
command query separation, the observability model — is built on twenty years of software
engineering experience. The patterns are well-understood, the trade-offs are familiar, and
the principal developer has high confidence in the design decisions, provided adequate rest
and first-principles discipline (ADR-007, ADR-008).

In this domain, precision is achievable and expected. An `EntityName` is a slug or a GUID,
never both. A command returns void. A repository has exactly three responsibilities. These
are not approximations — they are design invariants with sharp boundaries. The framework
ADRs reflect this confidence: they are decisive, specific, and closed.

### Natural science domains

The natural science modules — soil chemistry, entomology, botany, apiary management,
nutrient cycling, climate interaction — are built on the principal developer's environmental
science education and amateur practice, supplemented by published research and Claude's
ability to synthesise and present scientific concepts.

In this domain, humility is required. Soil nitrogen cycling involves microbial populations
that are measured with uncertainty ranges, not exact values. The biological amplification
factor for a given soil type is an empirical approximation, not a computed constant. An
optimal pH range for tomato cultivation is a consensus range, not a theorem.

The naturalist is, by definition, an amateur in many of these domains. The application
exists to support learning and observation, not to assert authority over natural processes.
Design decisions in the science modules must be held more lightly, supported by references,
and open to correction as understanding deepens.

### The need for a reference library

When the framework models a soil amendment's nitrogen contribution, or encodes the safety
threshold for a compound, or classifies an insect's ecological role, it is making claims
about the natural world. These claims must be traceable to their sources.

A naturalist who encounters `OptimumRanges.NITROGEN_LBS_PER_1000_SQFT` in the code should
be able to follow a reference chain to the research, extension publication, or field guide
that supports the value. Without this chain, the domain model becomes an oracle — a black
box that asserts facts without exposing the reasoning. An oracle is the opposite of what a
naturalist needs. A naturalist needs to study, question, and reason about the models that
guide observation.

Claude has demonstrated the ability to present physical and natural science concepts in a
digestible, accurate, multi-level format — from preschool wonder to university rigour (the
Durrell Description pattern). This ability is valuable precisely because it supports the
naturalist's learning process. But digestibility without traceability is still an oracle.
The reference library closes the loop: Claude explains the concept, the reference anchors
it in published knowledge, and the naturalist can deepen their understanding independently.

## Decision

### Confidence is domain-dependent

Design decisions carry an implicit confidence level based on the domain they belong to:

**High confidence** — framework architecture, module governance, identity model, repository
contracts, test infrastructure. These are closed decisions backed by extensive professional
experience. They are changed only when first-principles analysis reveals a structural error
(ADR-007), not because of uncertainty.

**Moderate confidence** — agronomic rules derived from well-established extension
publications and soil science textbooks. Optimal pH ranges, macronutrient thresholds,
amendment application rates. These are consensus values with broad support but regional
variation. They should cite their sources and acknowledge that local conditions (Oak Vista's
specific soil, climate, and management history) may shift the optimum.

**Low confidence** — emerging ecological models, microbial dynamics, complex nutrient
interactions, species behaviour under novel conditions. These are encoded as provisional
models, marked `@Incubating` (ADR-008), and expected to evolve as observation data
accumulates.

### Every scientific claim needs a reference

Domain constants, optimal ranges, safety thresholds, ecological classifications, and
agronomic rules must be traceable to a published source. The reference may be:

- A peer-reviewed paper or textbook (preferred for fundamental science)
- A university extension publication (preferred for regional agronomic practice)
- A manufacturer's technical data sheet (for product-specific data)
- A field guide or identification manual (for taxonomy and species behaviour)

The reference is not a formality. It is the mechanism by which the naturalist can evaluate,
question, and update the domain model. A value without a reference is a value that cannot
be challenged — and a value that cannot be challenged has no place in a naturalist's
toolkit.

### The reference library is a first-class domain concept

The application will maintain a reference library — a structured collection of sources
that the domain model draws upon. This is not a bibliography appendix; it is part of the
domain. When a naturalist views a compound's safety profile, the references that support
the safety thresholds should be accessible. When a naturalist questions why the model
recommends a particular amendment rate, the supporting publication should be one step away.

The reference library serves three purposes:

1. **Traceability** — every encoded scientific claim can be traced to its source
2. **Education** — the naturalist can study the source material to deepen understanding
3. **Correction** — when new research supersedes old, the reference chain identifies which
   domain values need updating

### Claude's role: explanation and synthesis, not authority

Claude's ability to present science concepts at multiple levels of understanding (the
Durrell Description pattern) is a core capability of the application. Claude explains;
the reference library provides the authority. Claude synthesises across sources; the
naturalist evaluates the synthesis against the referenced material.

This division is deliberate. Claude is a tool for understanding, not a substitute for the
naturalist's own reasoning. The goal is an informed naturalist who can evaluate the models,
not a dependent user who trusts the application without question.

## Consequences

- Framework ADRs are treated as high-confidence closed decisions; natural science domain
  models are treated as provisional and open to correction
- Every scientific constant, threshold, range, and classification in the domain code must
  cite a published source — inline documentation, Javadoc `@see` tags, or a structured
  reference entity
- The reference library is a planned first-class domain module, not an afterthought
- `@Incubating` (ADR-008) is used liberally in science modules to signal provisional models
- Claude's multi-level explanations (Durrell Description) continue as a core feature, with
  the explicit understanding that explanation is not authority — references provide authority
- Domain values derived from regional extension publications acknowledge that local
  conditions may require adjustment
- The naturalist is the final authority on whether a model fits their observations — the
  application supports reasoning, not compliance
