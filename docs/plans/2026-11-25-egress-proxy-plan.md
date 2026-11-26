# Egress Allowlist Proxy Implementation Plan (Java4Shell WI-3)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `app` reaches only `api.anthropic.com`, `challenges.cloudflare.com` and the mail host, through a Squid allowlist proxy, and has no other route out.

**Architecture:** An `internal: true` compose network holds `db`, `db-init` and `app`. A new `egress-proxy` service (Squid) sits on both that network and `default`, and allows only `CONNECT` to listed host:port pairs. The JVM's `https.proxy*` properties and Jakarta Mail's `mail.smtp.proxy.*` send traffic through it, and a test pins that the Turnstile `HttpClient` asks the default selector (it already does).

**Tech Stack:** docker compose, `ubuntu/squid` (Squid 6.14 on Ubuntu 24.04), Java 25 `java.net.http`, Spring Mail.

**Spec:** [2026-11-25-egress-proxy-design.md](2026-11-25-egress-proxy-design.md)

## Global Constraints

- Lock down `app`, `db`, `db-init` only. Alertmanager, cloudflared and Prometheus keep `default` access.
- Allowed tunnels: `api.anthropic.com:443`, `challenges.cloudflare.com:443`, `${SMTP_HOST}:${SMTP_PORT}`. Exact names, `dstdomain -n`, `CONNECT` only.
- Refuse private, loopback and link-local destinations: `10.0.0.0/8 172.16.0.0/12 192.168.0.0/16 127.0.0.0/8 169.254.0.0/16 fc00::/7 fe80::/10`.
- Image `ubuntu/squid:6.6-24.04_edge@sha256:d9c34c5ab74c6dd41cf1070d9b4221aac072e67e983527ca2d41512958f77506`. No Maven version is involved, so the root-pom rule is untouched.
- Proxy address `egress-proxy:3128`. No published port.
- Docker Engine floor 26 on the host.
- Never name a credential. Worktree installs pass `-Dmaven.repo.local=<worktree>/temp/m2`.
- Commit only when Pat says "commit".

## Review Focus

- A `SPRING_MAIL_HOST` that is unset locally: `start.sh` must still start Squid, with the mail rule matching nothing. Pinned by Task 2, step 4.
- An allowed name requested by IP literal (`CONNECT 160.79.104.10:443`): refused. Pinned by Task 2, step 4.
- An outside name resolved from inside `app` (DNS leak): fails on an internal network. Pinned by `check.sh` and the CI step.
- Plain HTTP `GET http://api.anthropic.com/` through the proxy: refused, because only `CONNECT` is allowed. Pinned by Task 2, step 4.
- Tests and plain `java -jar` runs with no proxy properties: Turnstile goes direct, unchanged. Pinned by Task 1's existing suite staying green.

---

### Task 1: Turnstile client honours the JVM proxy settings

**Files:**
- Modify: `adapters/challenge-turnstile/src/main/java/com/naturalist/challenge/turnstile/TurnstileChallengeVerifier.java` (the `http` field)
- Test: `adapters/challenge-turnstile/src/test/java/com/naturalist/challenge/turnstile/TurnstileChallengeVerifierTest.java`

**Interfaces:** none produced.

- [ ] **Step 1: Write the failing test** `asksTheDefaultProxySelectorWhereToConnect`. Install a recording `ProxySelector` (returns `List.of(Proxy.NO_PROXY)`, records each `URI`) with `ProxySelector.setDefault`, construct the verifier against the local `endpoint`, call `verify(submission("a-token"))`, and assert the recorded URIs contain `endpoint`. Restore the previous default in a `finally`.
- [ ] **Step 2: Run it and see it fail.** `mvn -q test -pl adapters/challenge-turnstile -Dtest=TurnstileChallengeVerifierTest -Dmaven.repo.local=$PWD/temp/m2`. Expected: the assertion fails, because no URI was recorded.
- [ ] **Step 3: Implement.** `HttpClient.newBuilder().proxy(ProxySelector.getDefault()).connectTimeout(ATTEMPT_TIMEOUT).build()`. Add a comment that the selector honours `https.proxyHost` (set in compose, ADR-029).
- [ ] **Step 4: Run the whole class and see it pass.**

### Task 2: The egress proxy and the networks

**Files:**
- Create: `deploy/egress-proxy/squid.conf.template`, `deploy/egress-proxy/start.sh` (executable), `deploy/egress-proxy/check.sh` (executable)
- Modify: `compose.yaml`, `compose.override.yaml`, `compose.production.yaml`, `.github/workflows/image.yml`

**Interfaces:**
- Produces: service `egress-proxy` on networks `internal` and `default`. `start.sh` reads `SMTP_HOST` and `SMTP_PORT` (both optional) and renders `@SMTP_HOST@` / `@SMTP_PORT@` into `/tmp/squid.conf`. `check.sh [service]` (default `app`) runs inside the named container through `docker compose exec`.

- [ ] **Step 1: `squid.conf.template`.** `http_port 3128`; ACLs and the `http_access` order from the spec (deny `!CONNECT`, deny private `dst`, allow `https_hosts`+port 443, allow `mail_host`+`mail_port`, deny all); `cache deny all`, `cache_mem 8 MB`, no `cache_dir`; `access_log stdio:/dev/stdout`; `cache_log /dev/stderr`; `pid_filename none`; `via off`; `forwarded_for delete`.
- [ ] **Step 2: `start.sh`.** POSIX `sh`, `set -eu`. With `SMTP_HOST` unset, render a mail rule that never matches (for example the name `mail.invalid`, port 1). Then `exec /usr/sbin/squid -f /tmp/squid.conf -NYC`. Same shape as `deploy/alertmanager/start-production.sh`.
- [ ] **Step 3: Compose.**
  - `compose.yaml`: `networks: internal: {internal: true}, default: {}`. Put `db`, `db-init` and `app` on `[internal]`. Put `prometheus` and `cloudflared` (in production) on `[internal, default]`.
  - Add `egress-proxy`: the image, `entrypoint: ["/etc/egress-proxy/start.sh"]`, the `deploy/egress-proxy` mount read-only, `networks: [internal, default]`, `read_only: true`, `tmpfs: [/tmp, /var/run, /var/spool/squid, /var/log/squid]`, `mem_limit: 128m`, `restart: unless-stopped`, and a healthcheck (a bash `/dev/tcp` probe of 3128).
  - `app` gets `JAVA_OPTS: -Dhttps.proxyHost=egress-proxy -Dhttps.proxyPort=3128` and `depends_on: egress-proxy: {condition: service_healthy}`.
  - Override: `app` and `db` on `[internal, default]` so their ports publish, and `mailpit` on `[internal, default]`.
  - Production: `egress-proxy.environment` `SMTP_HOST`/`SMTP_PORT` from the same `.env` keys as Alertmanager. `app` gets `SPRING_MAIL_PROPERTIES_MAIL_SMTP_PROXY_HOST: egress-proxy` and `..._PROXY_PORT: "3128"`. `cloudflared` gets `networks: [internal, default]`.
  - Comments say why each network membership exists.
- [ ] **Step 4: Prove the proxy locally.** `docker compose up -d egress-proxy`, then from `docker run --rm --network naturalist_internal --entrypoint bash eclipse-temurin:25.0.5.1_2-jre-noble -c '<probe>'`, expect:
  - `CONNECT api.anthropic.com:443` → `200`
  - `CONNECT example.com:443` → `403`
  - `CONNECT 169.254.169.254:80` → `403`
  - `CONNECT 160.79.104.10:443` → `403`
  - `GET http://api.anthropic.com/` → `403`
  - direct `/dev/tcp/1.1.1.1/443` → fails
  - `getent hosts example.com` → exit non-zero

  Also check that `docker compose logs egress-proxy` shows `TCP_DENIED` lines.
- [ ] **Step 5: `check.sh`.** It runs the probes above inside the `app` container. It adds a mail probe when `SPRING_MAIL_HOST` is set in the app's environment: `200` passes, `403` reads "proxy refused (config)", and `502`/`503` reads "host cannot reach the mail server (network or provider port block)". It exits non-zero on any unexpected answer.
- [ ] **Step 6: Run the full local stack.** `deploy/create-secrets.sh` if needed, `docker compose up -d --build`. The app becomes healthy, `deploy/egress-proxy/check.sh` passes, sign-up mail reaches Mailpit, and the proxy log shows no denials during startup.
- [ ] **Step 7: CI.** In `image.yml`, add `deploy/egress-proxy/**` to both `paths` lists. After the builds, add a step "Check the egress allowlist" that runs Step 4's probes, using `docker compose up -d --wait egress-proxy` and the built `naturalist-app:ci` image as the probe container on `naturalist_internal`. Validate with `docker compose config -q` for both file sets.

### Task 3: Records

**Files:**
- Create: `docs/adr/ADR-029-egress-allowlist.md`
- Modify: `docs/adr/README.md`, `docs/hosting-readiness.md` (Docker Engine ≥ 26 in section 3 or 4; `check.sh` plus identify, sign-up mail and sign-in in section 5; the DigitalOcean SMTP-port note at the email-provider row), `docs/runbooks/local-stack.md` (the proxy, how to add a host), `docs/plans/2026-11-19-java4shell-hardening-plan.md` (tick WI-3 and add a status line), `docs/work-tracker.md` (row #7)

- [ ] **Step 1: ADR-029.** Same short form as ADR-027/028: context (runtime RCE, the credential in process memory, hardening plan WI-3), decision (spec's Design in brief), consequences (adding a host = one line in `squid.conf.template` plus this ADR's host table; the EOL client will need one; Alertmanager and cloudflared are deliberately outside; Docker floor 26; data can still be pushed to an allowed host, bounded by ADR-028), and the rejected alternatives (tinyproxy, Smokescreen, SNI pass-through).
- [ ] **Step 2: The other docs,** as listed.

### Task 4: Gates

- [ ] `mvn install -DskipTests -Dmaven.repo.local=$PWD/temp/m2 && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true -Dmaven.repo.local=$PWD/temp/m2` → BUILD SUCCESS.
- [ ] `mvn verify -Dmaven.repo.local=$PWD/temp/m2` (background) → BUILD SUCCESS.
- [ ] The credential grep from CLAUDE.md → nothing.
- [ ] `docker build --pull --target app .` and `--target db-init` → succeed (image.yml's steps).
- [ ] ~~Observability review~~: not owed, since there is no `src/main/java` change (Task 1 ruling).
