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
| JDK | Both images use `eclipse-temurin:25-jre-noble`, inside the affected range | Exposed |
| Egress | `app` sits on Compose's default network and can open connections anywhere | Gap |
| Image rebuilds | Not checked whether CI rebuilds images on a schedule to pick up patched base images | Unknown |
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
- [ ] **WI-2: Get onto a patched JDK and keep it patched.**

  Files: `Dockerfile`, `.github/workflows/ci.yml`, plus a Renovate or Dependabot config. Pin the runtime by full version (e.g. `eclipse-temurin:25.0.x_y-jre-noble`) instead of the floating `25-jre-noble`, so a JDK change is a visible PR. Add automated update PRs for Docker base images. Check whether CI rebuilds on a schedule; if not, add a weekly build with `--pull`. Verify: Temurin's advisory names CVE-2026-114938 as fixed in the pinned version.
- [ ] **WI-3: Restrict outbound network access.**

  Files: `compose.yaml`, `compose.production.yaml`, new `deploy/egress-proxy/`. Put `app` and `db` on an `internal: true` network. Route `app`'s outbound HTTPS through a small forward proxy with a host allowlist: `api.anthropic.com`, the SMTP host, Cloudflare's Turnstile verification endpoint. `cloudflared` keeps its own network path to the edge. Configure the Anthropic OkHttp client, Spring Mail and the challenge client to use the proxy. Verify: from inside `app`, a request to an arbitrary host fails, while identification, mail and the login challenge still work. Record the decision as an ADR.
- [ ] **WI-4: Confirm seed passwords are throwaway.**

  Confirm the passwords behind the bcrypt hashes in `naturalist-credentials.json` are not used for any real account. No code change if they are not.

WI-5 to WI-8 are repository and host hygiene, independent of Java4Shell. WI-5 and WI-6 are code changes; WI-7 and WI-8 are manual settings a code session cannot make.

- [x] **WI-5: Add a security policy and private reporting.**

  Files: new `SECURITY.md`. State the supported branch, how to report privately, and an expected response time. Manual: enable GitHub private vulnerability reporting, secret scanning with push protection, Dependabot alerts (covers Maven dependencies, which WI-2 does not), and CodeQL for Java. Verify: the repo's Security tab shows all four enabled.
- [ ] **WI-6: Harden the CI workflow.**

  Files: `.github/workflows/ci.yml`. Add top-level `permissions: contents: read`. Pin `actions/checkout` and `actions/setup-java` by commit SHA with the tag in a comment, and let Dependabot's `github-actions` ecosystem update them. Never switch the trigger to `pull_request_target`. Verify: CI passes and the job log shows a read-only token.
- [x] **WI-7: Protect `main` (manual).**

  Require pull requests and a passing `ci` check before merge; block force-pushes and deletion. This is the backstop for agent-driven sessions.
- [ ] **WI-8: Lock down the host and review personal detail (manual).**

  Hetzner firewall: drop all inbound except SSH, ideally from known IPs only. SSH key-only, no password or root login. Enable unattended security upgrades. Separately, decide whether the Chico, apiary and "Oak Vista" references in domain docs and seed data are acceptable public detail, and keep future seed data and skill files at that level.

## Rejected alternatives

| Alternative | Why rejected |
| --- | --- |
| Make the repo private | Real threat is untargeted mass scanning of live hosts, which ignores source visibility. Security must hold with the code public (Kerckhoffs). The tunnel already hides the origin. |
| `-XX:UseAVX=2` as the mitigation | Disables all AVX-512 use, a wider performance cost than needed. Breaks JVM startup on ARM if the config is shared. `-XX:-UseBASE64Intrinsics` targets only the vulnerable path. |
| Move the key out of the JVM's reach (vault, rotation on every call) | Any secret source still ends up in process memory to call Anthropic, so RCE inside the JVM reads it regardless. Spend limits (ADR-028) and egress control (WI-3) bound the damage instead. |
| Rely on dependency scanning to detect runtime CVEs | The JDK is not declared in `pom.xml`, so manifest scanners miss it. Runtime versions are tracked through the pinned base image (WI-2). |
| Strip the production domain from `compose.production.yaml` | Low value: the domain is public anyway and the tunnel keeps the origin IP out of DNS. Revisit only if a published port is ever added. |

## Open questions

- [ ] Does the production Hetzner server's CPU report `avx512vbmi` in `/proc/cpuinfo`? If it is a CAX (ARM) server, Java4Shell does not apply at all.
- [ ] Which Temurin 25.x build is the confirmed fix for CVE-2026-114938?
- [ ] Does any transitive dependency decode Base64 from unauthenticated requests? A quick check: grep the resolved dependency tree, or run the app with a breakpoint on `java.util.Base64$Decoder.decode` while hitting public endpoints.
- [ ] Is the Anthropic workspace spend limit from ADR-028 actually set in the Console?

## Sources

- [Semgrep: Java4Shell, a critical Java vulnerability hiding in plain sight](https://semgrep.dev/blog/2026/java4shell-openjdk-base64-vulnerability/)
- [SentinelOne: Defending against Java4Shell](https://www.sentinelone.com/blog/java4shell-vulnerability-in-openjdk-enables-rce-on-avx-512-cpus/)
- [Endor Labs: Java4Shell unauthenticated RCE reported in OpenJDK 17](https://www.endorlabs.com/learn/java4shell-unauthenticated-rce-reported-in-openjdk-17-cve-2026-114938)
