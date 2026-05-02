# ADR-025: DI via Marker Annotations

**Status:** Accepted

**Date:** 2026-05-02

## Context

The catalog assembly composes a runtime out of three kinds of contribution
each domain ships from its `*-core` module: a `DomainId` subtype, a
`CatalogContribution`, and one or more `EntityReferences` providers. Plus,
on the management console, a per-domain `EntityRefLinker` that translates
catalog references into UI hyperlinks.

Before the runtime architecture refactor, the management console's
`CatalogConfiguration` declared a hand-written `@Bean` for every
contribution and provider in every active domain. Each new domain added
three or four `@Bean` methods. Every signature change to a contribution
constructor rippled into the configuration class. The composition root
became the place where adding a domain felt heaviest — exactly the
opposite of what the modular monolith promises.

The obvious fix — `@Component` on each contribution class plus a
`@ComponentScan` — is wrong. `@Component` is a Spring annotation. Putting
it on a class in `domains/<d>/<d>-core` would force Spring onto the
classpath of every consumer of that domain's core, including downstream
domains and tests. The boundary discipline that protects core from
infrastructure libraries (ADR-018) protects core from Spring with the same
force.

The codebase needs an annotation-driven registration story that works
through Spring without making domain code know that Spring is the runtime.

## Decision

### One marker in the kernel

`kernels/framework` ships a single marker annotation:

```java
@Retention(RUNTIME)
@Target(TYPE)
public @interface DomainService { }
```

Located at `com.naturalist.infrastructure.DomainService`. No
meta-annotations. No third-party imports. Pure marker. Domain `*-core`
code imports the marker only — never Spring, never any other DI
framework.

The marker's name is deliberately broad. A `DomainService` is *any* class
that participates in the runtime composition of a domain — a
contribution, an inverse-SPI provider, a domain identity record, a query
implementation, a repository mock, a UI linker. The single name covers
the catalog of roles the runtime adapter is expected to instantiate. A
finer-grained taxonomy (`@CatalogContribution`, `@DomainReference`,
`@DomainLinker`, …) was considered and rejected on the grounds that the
constructor signature already disambiguates the role at scan time.
Splitting the marker is deferred until a runtime needs to filter on
something the constructor cannot express.

### One adapter knows the runtime

`adapters/spring-runtime/` ships `DomainServiceScan`, an
`ImportBeanDefinitionRegistrar` that runs Spring's
`ClassPathScanningCandidateComponentProvider` filtered by an
`AnnotationTypeFilter(DomainService.class)`. Each candidate is registered
with the active `BeanDefinitionRegistry` under a generated bean name.

This is *not* `@ComponentScan`. Spring's `@ComponentScan` only matches
stereotypes meta-annotated with `@Component`; `@DomainService` carries no
such meta-annotation and never will. The registrar drives the scanner
directly so the marker stays free of Spring lineage.

The adapter depends on `kernels/framework` (for the marker definition)
and on `spring-context` (for the registrar machinery). It does not depend
on `spring-boot`. Apps choose Spring Boot for their own composition; the
adapter does not.

### App opt-in is a single `@Import`

A composition root activates the scan with one annotation:

```java
@Configuration
@Import(DomainServiceScan.class)
public class CatalogConfiguration {

    @Bean
    Catalog catalog(List<DomainId> domains,
                    List<CatalogContribution> contributions,
                    List<EntityReferences<?>> providers,
                    Resilience resilience) {
        return CatalogAssembly.from(domains, contributions, providers, resilience);
    }
}
```

The `@Bean` method that *assembles* the catalog stays in the app — it
encodes the composition policy (slug-uniqueness check, resilience wiring)
that no marker can encode. Everything that *contributes to* the catalog
is discovered.

### Scan scope

`DomainServiceScan` scans `com.naturalist` — the single root that covers
every domain api, every domain core, the kernels, and the adapters. A
contributing class is found wherever it lives. The cost of a broad scan
is paid once at startup; the rejection of every non-marked class is a
cheap classpath read.

When (or if) third-party plugins land, the scope narrows to a
`META-INF`-listed registration model (`AutoConfiguration.imports`-style
or a `ServiceLoader`-style file). Until then the broad scan is the right
default.

### Future runtime swap

If a non-Spring runtime ever lands — Guice, Helidon, plain `main()`
composition — the change is a single new module under
`adapters/runtime-<x>/`. That module ships its own equivalent of
`DomainServiceScan`. Domain code does not change. Apps choose which
runtime adapter they import. The kernel's marker is the contract; every
runtime adapter is a satisfaction of the contract.

## Consequences

- Domain `*-core` modules carry `@DomainService` on contributions,
  providers, `DomainId` subtypes, and any query implementation that
  takes a constructor parameter the runtime is expected to inject. They
  do not depend on Spring.
- The management console's `CatalogConfiguration` has exactly one
  `@Bean` method — the `Catalog` assembly itself. Every per-domain
  `@Bean` is gone.
- Adding a new domain to a deployment is: ship the domain's `*-api` and
  `*-core` modules with `@DomainService` on the right classes; declare
  the modules as dependencies of the app; the next startup discovers
  them. No edits to `CatalogConfiguration`.
- A class that is `@DomainService`-annotated but whose constructor
  cannot be satisfied at scan time is a deployment defect — Spring fails
  startup with a named offender. The same failure mode as a missing
  `@Bean`, surfaced earlier.
- A hypothetical Guice-based deployment is ~200 lines under
  `adapters/runtime-guice/`. Spring's hold on the system is the
  registrar, not the marker.
- Static composition (a unit test wanting a hand-curated subset of beans)
  is unchanged: that test does not import `DomainServiceScan` and wires
  the small set it needs by hand.

## Applicability Signals

Flag an ADR-025 violation in review when any of the following appears:

- A domain `*-core` `pom.xml` declares a Spring dependency.
- A domain class carries `@Component`, `@Service`, `@Repository`, or any
  other Spring stereotype.
- The `@DomainService` marker gains a Spring meta-annotation.
- An app's `CatalogConfiguration` (or equivalent) regains a per-domain
  `@Bean` for a contribution, provider, or `DomainId` subtype.
- A new DI runtime adapter lands outside `adapters/`, or a runtime
  adapter takes a dependency on a domain api or core.
- A class in `kernels/framework` imports `org.springframework.*`.

## Related ADRs

- ADR-018 — Third-Party Dependency Policy (Spring is forbidden in
  framework, api, and core; this ADR is the mechanism that keeps the
  rule satisfied while still using Spring as the runtime).
- ADR-023 — Open `DomainId` (per-domain `DomainId` subtypes are
  discovered through this marker).
- ADR-024 — Apps and Adapters Trees (the home of
  `adapters/spring-runtime/`).
- ADR-026 — Resilience as a First-Order Concern (the same boundary
  discipline applies to Resilience4j: one adapter knows the vendor).

## Reference Implementation

Post-M6 and M7 of the runtime architecture refactor:

- `kernels/framework/.../infrastructure/DomainService.java` — the
  marker; runtime retention, `TYPE` target, no meta-annotations.
- `adapters/spring-runtime/.../DomainServiceScan.java` — the
  registrar. `BASE_PACKAGES = List.of("com.naturalist")`.
- `apps/management-console/.../CatalogConfiguration.java` — single
  `@Bean Catalog catalog(...)` plus `@Import(DomainServiceScan.class)`.
- `domains/plants/plants-core/`, `domains/chemistry/chemistry-core/`,
  `domains/insects/insects-core/` — every catalog contribution, every
  inverse-SPI provider, every query implementation, every linker
  carries `@DomainService` (where it currently exists).
- `apps/management-console/.../CatalogConfigurationTest.java` —
  integration test boots the Spring context and asserts each domain's
  contributions, providers, `DomainId`, and linker resolve.
