# Apps

Composition-root deployment artifacts. An `apps/` member is the executable
the system actually deploys — a Spring Boot fat jar today, possibly a
plain `main()`-based daemon tomorrow. The tree exists so the codebase has
an unambiguous home for executables, separate from the kernels that
support them and the per-domain `*-console` libraries that contribute UI
fragments to a deployment.

## Placement rule

A module belongs in `apps/` when **all** of the following are true:

1. It declares a `main` (or a Spring Boot equivalent that the build
   packages as an executable jar / native image / container image).
2. It composes the system. It is the only module shape allowed to know
   about every domain it deploys, and the only module shape allowed to
   import an `adapters/` member.
3. It is a deployment artifact in its own right — somebody runs it. A
   library that happens to contribute templates, beans, or static assets
   is not an app.

## What does NOT belong here

- **Per-domain `*-console` libraries.** `domains/<d>/<d>-console/` is a
  contribution library — it ships JTE templates, a per-domain
  `EntityRefLinker`, and any UI helper the management console
  composites. It has no `main`. It stays under `domains/`.
- **Adapters.** A module wrapping a vendor library to satisfy a kernel
  or `*-api` port belongs in `adapters/` (ADR-024). Adapters never have
  a `main`.
- **Kernels.** Cross-cutting infrastructure with light dependencies stays
  in `kernels/`. An app that wants kernel infrastructure depends on the
  kernel; the kernel never depends on the app.
- **Domain modules.** A domain's `*-api`, `*-core`, `*-repository-test`,
  and `*-repository-rdms` modules stay under `domains/`. They are
  consumed by an app, not located inside it.

## Members

- `management-console/` — Spring Boot fat jar. Domain catalog UI for
  insects, plants, chemistry. Composes every active domain plus the
  catalog kernel, the spring-runtime adapter, and the
  resilience-resilience4j adapter. Sole app today.

Future members anticipated by the runtime architecture refactor plan
(`docs/plans/runtime-architecture-refactor.md`):
a sync daemon, an importer, a scheduled-task runner, a field-guide API.
Each future app is a new module under `apps/` with its own composition.

## Dependency rules

- An app depends on every domain it composes (`*-api` and `*-core`
  modules), the kernels it uses (`framework`, `framework-test`,
  `field-notes`, `taxonomy`, `catalog`, `catalog-inmem`), and the
  adapters it wires (`adapters/spring-runtime/`,
  `adapters/resilience-resilience4j/`, future infrastructure adapters).
- Apps do **not** depend on apps. Each app is a standalone deployment.
  Code shared between two apps lives in a kernel or an adapter, not in
  another app.
- Adapters do not depend on apps. The dependency direction is one-way:
  apps → adapters → kernels.
- An app's composition root (`@Configuration` classes, the `main`
  class, runtime-strategy registrations) is the only place that wires
  named resilience strategies, registers `DomainServiceScan`, and
  declares the `Catalog` `@Bean`. Domain modules contribute the parts;
  the app composes them.

## Composition pattern

Apps follow the same shape as `apps/management-console/`:

1. **`@Import(DomainServiceScan.class)`** on a configuration class
   activates the marker-based bean discovery (ADR-025). Every
   `@DomainService`-annotated class on the classpath is registered.
2. **A single `Catalog` `@Bean`** assembles the catalog from injected
   `List<DomainId>`, `List<CatalogContribution>`,
   `List<EntityReferences<?>>`, and `Resilience` (ADR-023).
3. **`ResilienceConfiguration`** registers each named
   `ResilienceConfig` and exposes the assembled `Resilience` bean
   sourced from `Resilience4jResilience` (ADR-026).
4. **App-specific configuration** (controllers, security, view
   templates, scheduled jobs) lives in the app's own packages.

A new app reusing this composition copies the pattern, not the
configuration class. Each app owns its own composition root.

## Client identity behind a proxy

Per-client controls (the `auth.login.client` / `auth.register.client` limiters, any later
per-address limiter) key on `ClientKey.of(request)`, which is `getRemoteAddr()` and nothing
else. The app never reads `CF-Connecting-IP`, `X-Forwarded-For` or `Forwarded` itself: a header
any client can send is not an identity, and only the container knows which peer sent it.
Decisions in [ADR-027](../docs/adr/ADR-027-cloudflare-edge-and-abuse-protection.md) D3/D4.

- **The container rewrites the address, for trusted peers only.** `management-console`'s
  `application.yml` sets `server.forward-headers-strategy: native` and
  `server.tomcat.remoteip.remote-ip-header: CF-Connecting-IP` (plus `protocol-header:
  X-Forwarded-Proto`). Tomcat's `RemoteIpValve` then rewrites `getRemoteAddr()` and the scheme,
  but only when the TCP peer matches `internal-proxies`. That is left at Boot's default
  (loopback, RFC 1918, CGNAT, link-local), because the Cloudflare Tunnel connector is local
  and is the only peer the origin has.
- **Never `framework`.** `ForwardedHeaderFilter` has no trusted-proxy list, so every client
  could choose its own rate-limit key.
- **Fronted without a tunnel?** Then `internal-proxies` must become Cloudflare's published
  ranges. Without that, every request keys to the proxy's address and a per-client ceiling
  becomes a global lockout. This is also why the client-address config ships before, or with,
  any `ClientKey`-keyed limiter.
- **IPv6 keys on the /64.** One subscriber holds a whole /64 and can rotate inside it for free,
  so `ClientKey` maps an IPv6 address to its /64 prefix. IPv4 is used as is.
- **The address is PII.** Observed decisions never carry it as a metric tag (unbounded
  cardinality, ADR-017); a per-client rejection logs it at `WARNING` instead.
- **Tests.** `ForwardedClientAddressTest` and `UntrustedPeerForwardedHeadersTest` boot the app
  on a real port, since MockMvc never passes through the valve. The test `application.yml`
  replaces the main one wholesale, so it repeats the `server` block. Keep the two in step.

## Resource discipline

The deployment artifact ships only the app's own resources under
`BOOT-INF/classes/` (Spring Boot) — `application.yml`, static assets,
the app's own JTE templates. Library jars carry their own resources
under `BOOT-INF/lib/`. After packaging, `unzip -l` on the fat jar
should show the app's `application*.yml` only once and at the app
level. A `application*.yml` shipping from a library module is a defect.

The runtime architecture refactor plan's M4 captures the discipline:
*"the fat jar's `BOOT-INF/classes/` contains only the app's own
resources; library jars sit independently in `BOOT-INF/lib/` with
unique filenames."*
