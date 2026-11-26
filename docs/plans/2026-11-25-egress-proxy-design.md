# Egress allowlist proxy: design (Java4Shell WI-3)

Status: draft for Pat's approval, 2026-11-25. Source: [Java4Shell hardening plan](2026-11-19-java4shell-hardening-plan.md), WI-3; [work tracker](../work-tracker.md) row #7.

## Intent

Someone who gets code running inside the `app` container should not be able to send the model
credential, user photos or database rows to a host of their choosing. The JDK is patched (WI-2), but
the next runtime RCE is a matter of time, so this limits what that RCE can do. It has to be in place
before public traffic.

## Goals

- `app` reaches only the hosts it needs: the model vendor's API, the Turnstile verification
  endpoint, and the mail host. Everything else is refused.
- `app` has no direct route out: no outside TCP, and no outside DNS lookups, which also closes DNS as a
  leak path.
- Each refused attempt is logged, so it can become an alert later.
- The local stack has the same shape as production.

## Non-goals

- Locking down the other containers (Pat, 2026-11-25). Alertmanager keeps direct outbound access for
  SMTP and the heartbeat ping (its SMTP client cannot use a proxy). cloudflared keeps its path to the
  edge. Prometheus and Postgres take no outside input.
- Decrypting or inspecting traffic. The proxy only tunnels: it never holds a TLS key.
- Limiting what can be sent *to* an allowed host. An attacker could still push data to the model
  vendor under our credential. ADR-028's spend limits bound that.

## Anti-goals

- Any change that leaves identification, sign-up mail or the login challenge silently broken in
  production. The verification below proves all three still work.
- A proxy that needs Java code changes for each new outside host. Adding a host is a one-line config
  change.

## What the code calls out to today

| Caller | Host | Protocol | Uses the JVM proxy settings? |
| --- | --- | --- | --- |
| `AnthropicVisionService`, `AnthropicTextGenerationService` (`AnthropicOkHttpClient`) | `api.anthropic.com` | HTTPS 443 | Yes. The SDK leaves OkHttp's proxy unset, so OkHttp uses `ProxySelector.getDefault()`. No code change. |
| `TurnstileChallengeVerifier` (`java.net.http.HttpClient`) | `challenges.cloudflare.com` | HTTPS 443 | Yes. A client built without `.proxy(...)` uses `ProxySelector.getDefault()` (corrected while building: the first draft said it went direct). A new test pins it. No code change. |
| `SmtpEmailSender` (Spring Mail, Jakarta Mail) | `${SPRING_MAIL_HOST}` | SMTP + STARTTLS, `${SPRING_MAIL_PORT}` | Not by the JVM settings, but Jakarta Mail tunnels through an HTTP proxy when `mail.smtp.proxy.host` / `.port` are set. Config only. |
| `EolClient` | none | none | Still a mock. When the real client lands, its host joins the allowlist. |

JDBC to `db` is a plain socket on the internal network and is untouched.

## Approaches

- **A. Squid forward proxy, Canonical's `ubuntu/squid` image. Chosen** (Pat, 2026-11-25). A maintained,
  pinnable image that Dependabot's `docker-compose` ecosystem can update; a single config file; it
  logs every request. Cost: large software of which we use a sliver; about 30–50 MB of memory.
- B. tinyproxy. Smaller and simple, but it has no official image, so we would build and pin our own on
  Alpine, and Dependabot can't update the package inside it. Weaker logs.
- C. Smokescreen (Stripe). Purpose-built and blocks private addresses natively, but it has no
  published image, so we would build a Go project from source.
- Ruled out: SNI pass-through (nginx `stream` + `ssl_preread`). It needs no client config, but it
  breaks SMTP STARTTLS, because the TLS hostname only appears after the plain-text start.

## Design

### Networks

Two networks in `compose.yaml`:

- `internal` (`internal: true`): no route out, and Docker's DNS answers container names only. That
  DNS behaviour holds from Docker Engine 26.0; before it, the daemon still forwards outside names.
  *My call:* `docs/hosting-readiness.md` states a floor of Docker Engine 26 (Docker's `docker-ce`
  packages, not Ubuntu's older `docker.io`), and the checks below test DNS, so an older engine fails
  loudly instead of leaking quietly. Local is on 29.9.
  Members: `db`, `db-init`, `app`, `egress-proxy`, `prometheus` (it scrapes `app`), `cloudflared`
  (it forwards to `app`), and locally `mailpit`.
- `default`: ordinary bridge with outbound access. Members: `egress-proxy`, `prometheus` (it reaches
  `alertmanager`), `alertmanager`, `cloudflared`, and locally `mailpit` (Alertmanager's local smarthost).

`app`, `db` and `db-init` are on `internal` only. A container does not route between its networks, so
`cloudflared` and `prometheus` sitting on both does not give `app` a path out.

### The proxy: `egress-proxy`

New directory `deploy/egress-proxy/` holding `squid.conf` and `start.sh`. The service:

- Image `ubuntu/squid:6.6-24.04_edge`, pinned by digest as well as tag, so a rebuild under the same
  tag arrives as a visible Dependabot PR. The image's version is not a Maven property, so the
  root-pom rule doesn't apply. It lives in the compose file like the other third-party images.
  *Finding:* Canonical's `_beta` tags stopped updating in 2025-11; the `_edge` tags are the ones it
  still rebuilds (`6.6-24.04_edge`, 2026-11-18). 24.04 is the LTS track.
- Listens on 3128, reachable only on `internal`. It publishes no port.
- `start.sh` renders the mail host and port into the config from the environment, then `exec`s
  Squid, following `deploy/alertmanager/start-production.sh`. With no mail host set it leaves the
  mail rule matching nothing.
- Memory-only, with caching off: `cache deny all`, a small `cache_mem`, no `cache_dir`. `mem_limit: 128m`.
- Access log to stdout, so `docker compose logs egress-proxy` shows each allowed and refused tunnel.

Access rules, in order:

1. Refuse anything but `CONNECT`. The app makes no plain-HTTP calls, so there is nothing to forward.
2. Refuse a destination address in a private, loopback or link-local range (`10/8`, `172.16/12`,
   `192.168/16`, `127/8`, `169.254/16`, `fc00::/7`, `fe80::/10`). This covers DigitalOcean's metadata
   address and an allowed name that has been made to resolve inward.
3. Allow `CONNECT` to `api.anthropic.com` or `challenges.cloudflare.com`, on port 443 only.
4. Allow `CONNECT` to the mail host, on the mail port only.
5. Refuse everything else.

Names are matched with `dstdomain -n`, so an IP literal never matches by reverse lookup. Each host is
an exact name, not a wildcard.

*Corrected in review:* rule 2 as written made Squid resolve every requested name, unlisted ones
included, which is a DNS leak through the proxy. The built config checks the name and port first and
the inward address last, on one line (`allow CONNECT https_hosts https_port !inward`), so only an
allowed name is ever resolved. The logs rotate every minute on a 16 MB tmpfs, and the healthcheck is
kept out of the access log.

### The app

- `compose.yaml`, `app`: `JAVA_OPTS: -Dhttps.proxyHost=egress-proxy -Dhttps.proxyPort=3128`. The
  Dockerfile entrypoint already passes `$JAVA_OPTS`. `depends_on: egress-proxy`.
- `TurnstileChallengeVerifier`: no change. The JDK client already asks the default selector, and
  `request_asksTheDefaultProxySelectorWhereToConnect` fails if anyone sets `NO_PROXY`. With no proxy
  properties set (tests, a plain local run) the selector answers `DIRECT`.
- `compose.production.yaml`, `app`: `SPRING_MAIL_PROPERTIES_MAIL_SMTP_PROXY_HOST: egress-proxy`,
  `SPRING_MAIL_PROPERTIES_MAIL_SMTP_PROXY_PORT: "3128"`. Locally, mail goes straight to Mailpit on
  `internal`.
- `compose.production.yaml`, `egress-proxy`: the mail host and port from the same `.env` values the app
  uses.

### Local stack

The local override publishes `app` on `127.0.0.1:8080` and `db` on `127.0.0.1:5433`, but Docker does
not publish a port for a container that is only on an `internal` network. So `compose.override.yaml`
also attaches `app` and `db` to `default`. *My call:* locally the app still sends everything through
the proxy, which proves the proxy wiring; the full no-route-out lockdown is real only in production,
and verified there.

### Verification

- **CI (`.github/workflows/image.yml`).** A new step starts `egress-proxy` alone on an internal
  network, and from a throwaway container on that network checks that `CONNECT api.anthropic.com:443`
  answers 200, `CONNECT example.com:443` and `CONNECT 169.254.169.254:80` answer 403, a direct
  connection out fails, and an outside name (`example.com`) does not resolve. The workflow's `paths` gain `deploy/egress-proxy/**`.
- **Script `deploy/egress-proxy/check.sh`.** The same checks run inside the running `app` container
  with bash `/dev/tcp` and `getent` (the runtime image has no curl or wget). It also tunnels to the
  mail host and port and tells the two failures apart: a 403 is the proxy refusing (our config), a
  502/503 is the host unable to reach the mail server (the network). Run it on the VM after deploy.
- **On the VM, once per deploy (Pat, launch phase 3):** run `check.sh`, then identify a photo, register
  an account (mail arrives), and sign in (the challenge passes). This is added to
  `docs/hosting-readiness.md` section 5.
- `mvn verify` and the rewrite dry run cover the new test. No `src/main/java` changes, so no observability review is owed.

### Mail port and DigitalOcean

The mail rule takes its port from `SPRING_MAIL_PORT`, so any port the provider offers works without a
config change. DigitalOcean blocks outbound SMTP ports (25, 465, 587) on new Droplets by default. That
is a constraint on the launch 2.4 email-provider choice, not on this design: pick a provider that
accepts port 2525 or ask DigitalOcean to unblock 587. Alertmanager's direct SMTP is subject to it too.
`check.sh`'s mail check is what makes this failure obvious instead of looking like a proxy fault.

### Records

- New ADR-029, "Egress allowlist for the app container", in `docs/adr/`, with a README row.
- WI-3's box in the hardening plan and tracker row #7 record the result.
- `docs/runbooks/local-stack.md` and `docs/hosting-readiness.md` mention the proxy and how to add a
  host.

## Left to the builder

- Exact Squid directive spelling, and whether `start.sh` writes ACL files or substitutes into
  `squid.conf`.
- Hardening the proxy container (`read_only`, `cap_drop`, `tmpfs` for its run directory) where Squid
  tolerates it.
- The digest to pin, resolved at build time.
