# ADR-025: DI via Marker Annotations

> [rationale](rationale/ADR-025-di-via-marker-annotations.md)

Domain code is wired into Spring through a kernel marker, not Spring stereotypes.

- **Marker.** `kernels/framework` ships
  `com.naturalist.infrastructure.@DomainService` —
  `@Retention(RUNTIME) @Target(TYPE)`, no meta-annotations, no third-party
  imports. Domain `*-core` imports the marker only.
- **Adapter.** `adapters/spring-runtime/` ships `DomainServiceScan`, an
  `ImportBeanDefinitionRegistrar` that runs Spring's
  `ClassPathScanningCandidateComponentProvider` filtered by an
  `AnnotationTypeFilter(DomainService.class)`. Not `@ComponentScan` —
  `@DomainService` deliberately carries no `@Component` lineage.
- **App opt-in.** A composition root activates with
  `@Import(DomainServiceScan.class)`. The single `@Bean` that *assembles*
  the catalog stays in the app; everything that *contributes to* it is
  discovered.
- **Scope.** `BASE_PACKAGES = List.of("com.naturalist")`. Narrows to a
  `META-INF` registration list when third-party plugins land.
- **Future runtime swap.** A non-Spring runtime is a single new module
  under `adapters/runtime-<x>/`. Domain code does not change.

### Review flags

- Domain `*-core` `pom.xml` declaring a Spring dependency.
- Domain class carrying `@Component`, `@Service`, `@Repository`, or any
  Spring stereotype.
- `@DomainService` gaining a Spring meta-annotation.
- An app's `CatalogConfiguration` regaining a per-domain `@Bean`.
- A class in `kernels/framework` importing `org.springframework.*`.
