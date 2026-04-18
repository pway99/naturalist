# Patrick Way — Global Claude Configuration

## About Me

Principal software engineer (20+ years), B.S. Environmental Science / Chemistry minor.
I value precision, first-principles reasoning, and honest pushback over agreement.
Do not validate — push back when my reasoning is wrong.

## Communication Style

- Be direct and precise — no preamble, no filler
- Use scientific terminology accurately
- Flag DAG violations, architecture violations, and domain model errors immediately
- Prefer prose over bullet points for explanations
- Short answers for simple questions; full depth for complex ones

## Java Conventions

- Java 17+ — use records, sealed interfaces, pattern matching, switch expressions
- Package-private by default within sub-contexts; public only when crossing boundaries
- Strongly typed identifiers always — never raw String or Long as entity reference
- EntityId\<Long\> on every entity — null in JSON catalogs, assigned by TestEntitySource or RDBMS
- Implement Entity, Aggregate, or ValueObject interfaces on every domain class
- No framework lock-in in the domain layer

## Architecture Non-Negotiables

- Modular monolith + hexagonal (ports and adapters)
- DAG module governance — call out cycles immediately
- Mock HashMap repositories before any infrastructure adapter
- Model/data separation: Java defines schema, JSON defines instances
- Sub-context boundaries enforced by Java package-private visibility

## Current Primary Project

The Naturalist application — com.naturalist
See project CLAUDE.md for full context.
All domain files are in ~/dev/naturalist/ (or wherever the project lives)

## Do Not

- Add unnecessary comments to obvious code
- Suggest frameworks before the domain model is stable
- Confuse volume measurements with weight for soil amendments
- Recommend lime for soil with pH 7.2 (Oak Vista soil is already optimal)
- Use S. feltiae for Small Hive Beetle control (only H. indica works)
