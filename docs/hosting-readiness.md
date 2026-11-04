# Hosting readiness

What must happen before the app takes public traffic at `backyardnaturalist.app`, in order. This
is a summary: each item links to the document that owns the detail. Status as of 2026-11-04.

The packaged jar runs in Docker with its own database, and the abuse protection and spend ceilings
are built. What remains is the code gaps below and the setup that only happens outside this repo.

## 1. Decide

| Decision | Options and context |
|---|---|
| Host | Undecided. The [secrets plan](plans/2026-11-02-secrets-and-endpoint-protection-plan.md) suggests Fly.io, then Cloud Run. It must offer Postgres, a persistent disk or an image-store alternative, and secret injection. |
| Email provider | Undecided. It needs a sending domain with SPF and DKIM. |
| Model spend limit | The secrets plan proposes $60 a month for the production workspace. |
| Who gets identification | `naturalist.access.grant-policy` is `self-serve`, so every registrant gets `VISION`, within the usage budgets. Confirm this is right for launch. |
| Outside heartbeat check | Undecided. It receives the `Watchdog` alert every few minutes and must alert you when it stops, because nothing on the host can report the host dying. |

## 2. Close the code gaps

- [x] **Create a production database.** The `db-init` service creates the schema without dropping
  anything and loads the public catalog with no accounts. It refuses a database it didn't initialize
  ([local stack runbook](runbooks/local-stack.md)).
- [ ] **Apply later schema changes: blocks the first schema change after launch.** The project has
  no migration tool.
- [ ] **Review the seed field observations before going public.** The catalog load includes them,
  because catalog photos hang off them. They name real observers and places, such as a town, and they
  appear on public pages.
- [ ] **Strip photo metadata: blocks public hosting.** With JavaScript off, or on a direct POST,
  uploads keep their EXIF and GPS data and are served publicly with one-year caching
  ([PL-17](notes/parking-lot.md)).
- [ ] **Account recovery.** There is no password reset, and `/reset/**` has no controller behind it.
  Someone who registers another person's email can lock them out of that address
  ([S1](notes/2026-11-03-security-review.md)). Reset would fix this, including ending sessions on
  reset and suspend. The cheaper option is new wording on the confirm page plus purging accounts
  never verified after 24 hours, but forgotten passwords would remain unrecoverable.
- [ ] **Measure image reads under load.** Nothing caps concurrent `image.store` work except its
  callers, so measure a gallery-sized read ([PL-16](notes/parking-lot.md)).
- [ ] **Push `main`.** All of this work exists only locally, and CI hasn't run `PackagedJarIT` yet.

## 3. Set up outside the repo

The first two need no host and can start now.

- [ ] **Model vendor** ([secrets plan](plans/2026-11-02-secrets-and-endpoint-protection-plan.md),
  step 0): separate production and development workspaces, a spend limit on each, a scoped
  `model-credential` for each, and retire the current one. Record each in the
  [rotation log](runbooks/secret-rotation.md).
- [ ] **Cloudflare** ([edge runbook](runbooks/cloudflare-edge.md), steps 1, 3 and 4): zone and TLS,
  the challenge widget, and the security rules.
- [ ] **Host:** Postgres with backups, persistent storage for `naturalist.images.root`, and secret
  injection.
- [ ] **Tunnel** (edge runbook, step 2), with no inbound ports on the origin.
- [ ] **Email:** the sending domain, and a real `naturalist.email.from`.
- [ ] **Heartbeat:** create the outside check, and put its private address in `secrets/heartbeat-url`.

## 4. Configure the deployment

Pass these at deploy time, with secrets from the host's secret store. Never set either `log-only`
flag: they print emailed tokens and turn the challenge off.

| Setting | Value |
|---|---|
| `spring.datasource.*` | The production database. The main `application.yml` still points at the local test database. |
| `spring.mail.*` | The email provider |
| `naturalist.email.public-url` | `https://backyardnaturalist.app` |
| `naturalist.admin.username`, `naturalist.admin.password` | A real admin account, not a README example |
| `naturalist.vision.provider` | `anthropic`, plus the `model-credential` secret |
| `naturalist.secrets.source` | `environment` or `file`, depending on the host |
| `naturalist.challenge.site-key`, `naturalist.challenge.expected-hostname` | From the widget, and `backyardnaturalist.app`, plus the `challenge-credential` secret |
| `naturalist.images.root` | The persistent storage path |
| `ALERT_EMAIL_TO` | Where alerts go. `compose.production.yaml` requires it |

## 5. Prove it before going public

Run these against the deployment while the address is still unannounced.

- [ ] **A real login and sign-up through the real widget.** Test keys can't prove this because they
  return no action. It can be rehearsed before a host exists by running the jar behind a tunnel
  from a laptop, with throwaway data.
- [ ] **The [edge runbook's](runbooks/cloudflare-edge.md) post-deploy checks,** including that the
  origin refuses direct connections.
- [ ] **The session cookie is `Secure`** when served through the tunnel (S2). It depends on the
  forwarded scheme and hasn't been observed yet.
- [ ] **Forged client addresses are ignored** (secrets plan, step 8). As written, that check posts
  to `/login` from a script, which rule E2 now challenges first, so it needs rewriting to carry a
  browser's clearance.
- [ ] **One real identification,** with `/admin/resilience` listing `vision.identification` and
  `vision.identification.client`.
- [ ] **Real email:** a verification message arrives and its link works.
- [ ] **Credential rotation rehearsal** ([runbook](runbooks/secret-rotation.md)), then rotate after it.
- [ ] **Restore a database backup** at least once.
- [ ] **Alerts arrive by real email.** The rules are written and tested (below), but delivery has only
  been seen in Mailpit.
- [ ] **The heartbeat check alerts** when Alertmanager is stopped.
- [ ] **The tunnel can't reach metrics:** `https://backyardnaturalist.app/actuator/prometheus` is refused.

## Already done

- Packaged jar renders pages, guarded by `PackagedJarIT`.
- Edge design, and the [runbook](runbooks/cloudflare-edge.md) to stand it up (ADR-027).
- Bot challenge on login and registration, failing closed, with per-client and global rate limits.
- Secrets through `SecretProvider`, and layered spend ceilings for identification (ADR-028).
- Client address trusted only from the local connector.
- Metrics on a private port, read by Prometheus, with alerts through Alertmanager
  ([local stack runbook](runbooks/local-stack.md)).
- A [security review](notes/2026-11-03-security-review.md) with no findings at its reporting bar, apart from the items above.
