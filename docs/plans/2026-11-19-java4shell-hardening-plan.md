# Naturalist: Java4Shell hardening plan

Nov 19, 2026 · @Patrick

## Context

The repo's posture is sound; the one live exposure is the Java 25 runtime, plus one missing control (egress). This plan is meant to be handed to a fresh Claude Code session in `pway99/naturalist`.

Java4Shell (CVE-2026-114938) was disclosed by Calif on 2026-11-17. It is an integer underflow in HotSpot's AVX-512 Base64 decode intrinsic that causes an out-of-bounds heap write, reported as unauthenticated RCE (CVSS 4.0: 9.2). Affected: HotSpot Java 17.0.6+ and 18+, on x86-64 CPUs with AVX-512 VBMI. Not affected: Java 8, 11, 17.0.0 to 17.0.5, native ARM64 JVMs. As of 2026-11-19 vendor fix status was unconfirmed: Oracle's November update shipped, but no advisory named this CVE.

Interim mitigation: `-XX:-UseBASE64Intrinsics`, which routes decoding back to the bounds-checked pure-Java implementation.

## Findings

Reviewed on 2026-11-19: Dockerfile, the three compose files, `SecurityConfiguration`, the Anthropic adapters, and all 1,039 commits of history.

| Area | Finding | Status |
| --- | --- | --- |
| JDK | Both images used `eclipse-temurin:25-jre-noble`, inside the affected range. Now pinned to `25.0.5.1_2-jre-noble`, the fixed build (WI-2, 2026-11-25) | Fixed |
| Egress | `app` sits on Compose's default network and can open connections anywhere | Gap |
| Image rebuilds | CI (`ci.yml`) ran Maven only and never built the Docker image. `image.yml` now builds both images with `--pull` on every PR that touches them. A scheduled rebuild waits for production (WI-2, 2026-11-25) | Partly fixed |
| Base64 decode paths | Own code only encodes (`JdkSecureTokens`, `AnthropicVisionService`). No JWT library, no `httpBasic`, no remember-me; form login with server sessions | Low exposure |
| Anthropic key | Compose secret file `model-credential` via `FileSecretProvider`, not an env var; ADR-028 workspace spend limit | Sound |
| Git history | No Anthropic, GitHub or AWS key patterns; `secrets/` and `.env` never committed | Clean |
| Seed accounts | `naturalist-credentials.json` holds bcrypt hashes for test users incl. `patrick-way`; `DeploymentDbInitializer` loads no accounts | Fine if passwords are not reused |
| Origin exposure | Cloudflare tunnel only, no published ports; origin IP never appears in DNS | Sound |

Transitive libraries were not audited for `java.util.Base64` decode on unauthenticated input.

## Work items

Do these in order. WI-1 is the stopgap; WI-2 removes the need for it; WI-3 limits the damage of any future runtime RCE.

- [x] **WI-1: Disable the Base64 intrinsic.**

  Files: `compose.yaml`. Add `JAVA_OPTS: -XX:-UseBASE64Intrinsics` to the `app` service, and append it to the existing `db-init` `JAVA_OPTS`. The Dockerfile entrypoints already pass `$JAVA_OPTS` through. Do not use `-XX:UseAVX=2`; see Rejected alternatives. Verify: in the running container, `java -XX:+PrintFlagsFinal -version` (with the same opts) shows `UseBASE64Intrinsics = false`, and the app starts healthy. Remove once WI-2 lands on a confirmed fixed build.

  Done 2026-11-19 (`f8d9de23`). **Retired 2026-11-25 by WI-2:** the flag is gone from `compose.yaml`, because the pinned runtime is the build Adoptium names as fixed.
- [x] **WI-2: Get onto a patched JDK and keep it patched.**

  Files: `Dockerfile`, `.github/workflows/ci.yml`, plus a Renovate or Dependabot config. Pin the runtime by full version (e.g. `eclipse-temurin:25.0.x_y-jre-noble`) instead of the floating `25-jre-noble`, so a JDK change is a visible PR. Add automated update PRs for Docker base images. Check whether CI rebuilds on a schedule; if not, add a weekly build with `--pull`. Verify: Temurin's advisory names CVE-2026-114938 as fixed in the pinned version.

  Status 2026-11-25 (`72247a8f`..`5b3f77c5`, merged to main):
  - [x] Runtime pinned. Both `db-init` and `app` stages use `eclipse-temurin:25.0.5.1_2-jre-noble`. Adoptium's [guidance for CVE-2026-114938](https://adoptium.net/news/2026/11/temurin-november-2026-cspu-java4shell) (2026-11-18) lists JDK 25.0.5.1+2 as the Temurin 25 build with the fix. The Docker Hub tag was published 2026-11-20. The build stage is pinned too, to `maven:3.9.16-eclipse-temurin-25-noble`, the image the floating `3.9-eclipse-temurin-25` already resolved to.
  - [x] WI-1 flag removed, since the pinned build is confirmed fixed.
  - [x] `.sdkmanrc` moved to `25.0.5.1+2-tem`, matching the image. CI's `setup-java` keeps `java-version: '25'`, which resolves to the newest Temurin 25 GA (25.0.5.1+2 today). CI only runs the tests; the deployed runtime is pinned in the Dockerfile.
  - [x] Update PRs: `.github/dependabot.yml` covers `docker` (Dockerfile), `docker-compose` (the third-party images in the compose files), `github-actions` and `maven`, weekly, grouped. In Maven, Dependabot edits the `${...}` property in the root pom, so the "versions live only in root `<properties>`" rule holds. The Boot-managed properties are ignored, because they move with `spring-boot.version`.
  - [x] Image build in CI, triggered by PRs rather than a schedule (Pat's call, 2026-11-25). CI never built the image, so a separate `.github/workflows/image.yml` now does. It runs on `pull_request`, and on `push` to `main`, when `Dockerfile`, `.dockerignore`, `compose*.yaml` or the workflow itself changes, plus `workflow_dispatch`. It uses `permissions: contents: read` and the SHA-pinned checkout. It checks the compose files (production for structure only), runs `docker build --pull` for `db-init` and `app`, and prints the runtime `java -version`. It pushes nothing. This means every Dependabot base-image PR is proven to build before it merges.
  - [ ] Weekly `--pull` rebuild: deferred until production. Pinned tags mean `--pull` only picks up images re-published under the same tag (OS-layer patches), and that pays off only once a rebuilt image is deployed. Add the schedule as part of the production rebuild-and-redeploy job, not as a build-and-discard run.
- [ ] **WI-3: Restrict outbound network access.**

  Files: `compose.yaml`, `compose.production.yaml`, new `deploy/egress-proxy/`. Put `app` and `db` on an `internal: true` network. Route `app`'s outbound HTTPS through a small forward proxy with a host allowlist: `api.anthropic.com`, the SMTP host, Cloudflare's Turnstile verification endpoint. `cloudflared` keeps its own network path to the edge. Configure the Anthropic OkHttp client, Spring Mail and the challenge client to use the proxy. Verify: from inside `app`, a request to an arbitrary host fails, while identification, mail and the login challenge still work. Record the decision as an ADR.
- [x] **WI-4: Confirm seed passwords are throwaway.**

  Confirm the passwords behind the bcrypt hashes in `naturalist-credentials.json` are not used for any real account. No code change if they are not.

  Done 2026-11-25, no code change. Pat confirmed none of the seed passwords protects a real account. That covers two sets:
  - `domains/accounts/accounts-repository-test/src/main/resources/account/accounts.json`, the successor file: all five accounts share one bcrypt hash (`4baedf13`). Its plaintext is written nowhere in the repo, and no test depends on it. The login tests (`NaturalistLoginWebMvcTest`, `AdminAccountsControllerWebMvcTest`) re-encode their own throwaway password and restore the fixture afterwards.
  - The four hashes in the deleted `naturalist-credentials.json` (2026-07-06 to 2026-08-31). They are still in public git history, and each matches the plaintext in the `SeedHashPrinter` step of `2026-07-06-naturalist-auth-plan.md`, so they were public anyway.

  Seed accounts never reach production. `DeploymentDbInitializer` (the Dockerfile's `db-init` entrypoint) applies the `accounts` DDL only and loads catalog and usage data; `AccountSeeding` runs only from `TestDbSeeder`. The db-init jar still bundles `accounts-repository-test` and its `accounts.json` through `test-db-seeder`'s dependencies, but nothing in the deployment path reads them. No production code outside `test-db-seeder` references `AccountTestEntitySource`. `management-console` takes `accounts-repository-test` at test scope only.

WI-5 to WI-8 are repository and host hygiene, independent of Java4Shell. WI-5 and WI-6 are code changes; WI-7 and WI-8 are manual settings a code session cannot make.

- [x] **WI-5: Add a security policy and private reporting.**

  Files: new `SECURITY.md`. State the supported branch, how to report privately, and an expected response time. Manual: enable GitHub private vulnerability reporting, secret scanning with push protection, Dependabot alerts (covers Maven dependencies, which WI-2 does not), and CodeQL for Java. Verify: the repo's Security tab shows all four enabled.
- [x] **WI-6: Harden the CI workflow.**

  Files: `.github/workflows/ci.yml`. Add top-level `permissions: contents: read`. Pin `actions/checkout` and `actions/setup-java` by commit SHA with the tag in a comment, and let Dependabot's `github-actions` ecosystem update them. Never switch the trigger to `pull_request_target`. Verify: CI passes and the job log shows a read-only token.

  Done: read-only token `9e27d97f`. 2026-11-25: `actions/checkout@11d5960a…` (v4.4.0) and `actions/setup-java@cf277c60…` (v4.9.1) are pinned by full SHA with the tag in a comment, resolved with `gh api` from the official repos. Each SHA is the one the floating `v4` tag pointed to, so behaviour is unchanged. The `github-actions` ecosystem in `dependabot.yml` will offer the v7 checkout and v6 setup-java majors. The triggers stay `push`, `pull_request` and `workflow_dispatch`.
- [x] **WI-7: Protect `main` (manual).**

  Require pull requests and a passing `ci` check before merge; block force-pushes and deletion. This is the backstop for agent-driven sessions.
- [ ] **WI-8: Lock down the host and review personal detail (manual).**

  The host is a DigitalOcean Droplet (launch 2.3, decided 2026-11-25). DigitalOcean cloud firewall: drop all inbound except SSH, ideally from known IPs only. SSH key-only, no password or root login. Enable unattended security upgrades. Separately, decide whether the Chico, apiary and "Oak Vista" references in domain docs and seed data are acceptable public detail, and keep future seed data and skill files at that level.

## Rejected alternatives

| Alternative | Why rejected |
| --- | --- |
| Make the repo private | Real threat is untargeted mass scanning of live hosts, which ignores source visibility. Security must hold with the code public (Kerckhoffs). The tunnel already hides the origin. |
| `-XX:UseAVX=2` as the mitigation | Disables all AVX-512 use, a wider performance cost than needed. Breaks JVM startup on ARM if the config is shared. `-XX:-UseBASE64Intrinsics` targets only the vulnerable path. |
| Move the key out of the JVM's reach (vault, rotation on every call) | Any secret source still ends up in process memory to call Anthropic, so RCE inside the JVM reads it regardless. Spend limits (ADR-028) and egress control (WI-3) bound the damage instead. |
| Rely on dependency scanning to detect runtime CVEs | The JDK is not declared in `pom.xml`, so manifest scanners miss it. Runtime versions are tracked through the pinned base image (WI-2). |
| Strip the production domain from `compose.production.yaml` | Low value: the domain is public anyway and the tunnel keeps the origin IP out of DNS. Revisit only if a published port is ever added. |

## Open questions

- [x] Which host, and is it ARM? Decided 2026-11-25 (launch 2.3): a DigitalOcean 4 GiB Droplet, x86, US region, not Hetzner. It is not ARM, so Java4Shell can apply. WI-2 has since pinned the fixed Temurin build and retired the WI-1 flag. Nothing in the Dockerfile or compose files is x86-only, so an ARM host remains a fallback.
- [ ] Does the Droplet's CPU report `avx512vbmi` in `/proc/cpuinfo`? Check once the VM exists (launch 3.1). It no longer decides anything: the pinned 25.0.5.1+2 runtime is fixed either way.
- [x] Which Temurin 25.x build is the confirmed fix for CVE-2026-114938? **25.0.5.1+2** (Docker tag `25.0.5.1_2`), per Adoptium's 2026-11-18 guidance.
- [ ] Does any transitive dependency decode Base64 from unauthenticated requests? A quick check: grep the resolved dependency tree, or run the app with a breakpoint on `java.util.Base64$Decoder.decode` while hitting public endpoints.
- [ ] Is the Anthropic workspace spend limit from ADR-028 actually set in the Console?

## Sources

- [Adoptium: Eclipse Temurin guidance for CVE-2026-114938 "java4shell"](https://adoptium.net/news/2026/11/temurin-november-2026-cspu-java4shell)
- [Semgrep: Java4Shell, a critical Java vulnerability hiding in plain sight](https://semgrep.dev/blog/2026/java4shell-openjdk-base64-vulnerability/)
- [SentinelOne: Defending against Java4Shell](https://www.sentinelone.com/blog/java4shell-vulnerability-in-openjdk-enables-rce-on-avx-512-cpus/)
- [Endor Labs: Java4Shell unauthenticated RCE reported in OpenJDK 17](https://www.endorlabs.com/learn/java4shell-unauthenticated-rce-reported-in-openjdk-17-cve-2026-114938)
