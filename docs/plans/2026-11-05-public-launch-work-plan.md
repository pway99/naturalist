# Public launch work plan

The order of work to get the app public at `backyardnaturalist.app`. The
[hosting readiness checklist](../hosting-readiness.md) owns the detail of each item; this plan owns
the order. Items marked **new** aren't on the checklist and are owned here. Written 2026-11-05.

**Done when:** every item through phase 4 is closed, and the address is announced.

Sizes are rough guesses: **S** is under a session, **M** is about one session, **L** is several.

## Phase 0: Trust the build

| # | Item | Who | Size |
|---|---|---|---|
| 0.1 | ✅ 2026-11-08 **new** Find out why CI has never run. The repository's Actions were behind the Actions tab's "enable workflows" gate: push events were dropped silently while the API reported Actions enabled. Enabled by Pat; first run `b50a2e3f` green in 4m22s, `PackagedJarIT` and the alert rule tests included. `ci` gained `workflow_dispatch`. | Claude | S |

## Phase 1: Fix the code

These don't need a host, and most are privacy or account problems that get harder to fix once the
site is public.

| # | Item | Who | Size | Note |
|---|---|---|---|---|
| 1.1 | ✅ 2026-11-09 Strip photo metadata on upload ([PL-17](../notes/parking-lot-resolved.md)) | Claude | S–M | Metadata-only rewrite in the insects commands and in the image store; seed photos rewritten. The app keeps only a coarse grid-cell area ([design](2026-11-08-photo-location-privacy-design.md), [plan](2026-11-08-photo-location-privacy-plan.md)). |
| 1.2 | ✅ 2026-11-25 Review the seed field observations | Pat | S–M | Observation text (location, notes, identification) renders only to the observation's owner, and the deployment loads no naturalists or accounts, so none of it reaches the live site; it is public on GitHub. Pat's decisions: every Oak Vista / Chico `location` (9) is now "Sacramento Valley, CA" and the 3 empty ones null; the 3 identification evidence sentences naming Chico or Oak Vista are generalized; note typos fixed; observer slugs kept. The publicly rendered species `sightingNotes` that name Oak Vista and Chico belong to the Oak Vista sweep (tracker #6). |
| 1.3 | ✅ 2026-11-10 **new** Find out why 11 of 22 insect seed photos are missing | Claude | S | None were lost. The 11 were console uploads captured into `insect-images.json` (UUID names), whose photos were tracked under `data/images/insects/` instead of the classpath `insects/images/`. `db-init` counted them missing in its classpath pass, then copied them from a second `data/images` copy baked into the image. They now live on the classpath with the other seed photos, `data/images` is untracked and no longer shipped, and a seeded image row without a photo fails `db-init` (decided: yes, because it would be a broken image on a public page). |
| 1.4 | ✅ 2026-11-10 **new** Keep the alert address private: remove it from `application.yml`, and read it from a `secrets/alert-email` file for both the app and Alertmanager | Claude | S | Done: `application.yml` ships the placeholder `operator@naturalist.local` (the local Alertmanager's too); production mounts `secrets/alert-email` as `naturalist.usage.alert-email` through the existing configtree, and `start-production.sh` reads the same file, refusing an empty one. `ALERT_EMAIL_TO` is gone. **Pat still owes, before production:** an address at `backyardnaturalist.app` in `secrets/alert-email` on the host. The old personal address left in older plan docs and git history is already public, so it isn't scrubbed. |
| 1.5 | ✅ 2026-11-25 Account recovery (decision A) | Claude | S | Closes the email squatting finding, S1. Decided 2026-11-25: the cheap option. The confirm page (`verify.jte`) tells a reader who didn't sign up not to confirm. An hourly job (`UnverifiedAccountPurgeJob`, `naturalist.accounts.purge-cron`, `-` turns it off) purges registrations never verified once every verification link has expired: `AccountCommand.purgeLapsedRegistrations` deletes the tokens, then the account, never a verified, VISION or suspended one. `UnverifiedAccountPurgeTransaction` then deletes the linked naturalist in the same transaction, which frees the email and the handle. Decided with Pat 2026-11-25 (option A of three): member writes (sightings, photos, notes) now need a verified email, through a `VERIFIED` authority, so a never-verified account owns no content and is purged whole. Real password reset is 5.4, before opening up. |
| 1.6 | ✅ 2026-11-10 Measure a gallery-sized image read ([PL-16](../notes/parking-lot-resolved.md)) | Claude | S | Buffering whole photos ran the heap out at 100 concurrent 20 MB reads and stopped the app. A bulkhead alone didn't help: the bytes outlive the read while slow clients download. Reads now stream from disk: 200 concurrent 20 MB reads all served, heap peak 154 MB, pages unaffected. The classpath seed-photo fallback, dead in the packaged jar, is gone. Downscaling uploads on store would save bandwidth; not planned. |
| 1.7 | ✅ 2026-11-14 **new** Photo sizes | Claude | M | A stored photo is at most 1024 px on its longest edge and has a 640 px JPEG tile, which the galleries and cards show; the full photo opens over the page. Unstorable uploads are refused before identification is charged (`requireStorable` reads the header only; a body that does not decode is refused by the store, which the identify command now calls before it reserves the budget, so every store refusal is mapped before any charge), stores run under the `image.store.write` bulkhead and write each file atomically (temp file + `ATOMIC_MOVE`, tile before photo), and `conform` brings existing photos into line on every seed and `db-init` ([design](2026-11-11-photo-sizes-design.md), [plan](2026-11-11-photo-sizes-plan.md)). |
| 1.8 | **new** Watch storage capacity | Claude | S–M | Filling the disk stops the whole system: photos (`naturalist.images.root`), Postgres and logs share the host. Alert well before the disk is full, on free space of the image-store and database volumes, and count failed photo deletes (observability rule J3, `storedDiscarded`), whose leftover files accumulate. Pat asked for this 2026-11-16. |
| 1.9 | ✅ 2026-11-25 **new** Admin accounts page, so no account change needs direct SQL on the production database | Claude | M | `/admin/accounts` (ADMIN only, `AdminAccountsController`): a paged list (`AccountQuery.findPage` with `PageRequest.console`; the inherited paged read already had its `LIMIT n+1` rdbms adapter and contract tests, so the accounts domain is unchanged) showing email, verified, access and status, with CSRF-protected POST forms to grant and revoke `VISION` and to suspend and reinstate. Suspend then ends every live session of the account: `console.auth.AccountSessions.endAll(email)` over Spring's `SessionRegistry` (`SessionConfiguration`, with `HttpSessionEventPublisher`), wired in `SecurityConfiguration` as unlimited `maximumSessions` with `expiredUrl("/login?expired")`; registration's auto-login registers its session through `AccountSessions.register`. 5.4 reuses `endAll` after a reset. Revoke signs nobody out: the session keeps `VISION` until the next sign-in. Makes `request` a workable answer to 2.6. |

## Phase 2: Decide and set up outside accounts

These need no code and can run alongside phase 1.

| # | Item | Who | Size | Note |
|---|---|---|---|---|
| 2.1 | Model vendor: separate workspaces, spend limits, scoped credentials, retire the current one | Pat | S | Needs the spend limit decision. The plan proposes $60 a month. |
| 2.2 | Cloudflare: zone, TLS, challenge widget, security rules | Pat | S–M | [Edge runbook](../runbooks/cloudflare-edge.md), steps 1, 3 and 4. |
| 2.3 | ✅ 2026-11-25 Pick the host | Pat | S | DigitalOcean 4 GiB Droplet, x86, US region, about $29–31 a month, as the research recommends (`temp/reports/Naturalist hosting options compared.md`). x86 means Java4Shell can apply: keep the WI-1 flag until a fixed Temurin is pinned, and check `/proc/cpuinfo` for `avx512vbmi` once the VM exists. |
| 2.4 | Pick the email provider, and set up the sending domain with SPF and DKIM | Pat | S | |
| 2.5 | Pick the outside heartbeat service, and create the check | Pat | S | Receives `Watchdog`. |
| 2.6 | Confirm who gets identification at launch | Pat | S | `self-serve` gives every registrant `VISION`, within the usage budgets. Decision C (small invited group) reopens this: registration is open, so with `self-serve` "invited" only means unannounced. `request` holds `VISION` back until it is granted, and since 1.9 shipped (2026-11-25) an admin grants it at `/admin/accounts`. |

## Phase 3: Stand up production

Needs 2.2–2.5.

| # | Item | Who | Size | Note |
|---|---|---|---|---|
| 3.1 | Create the VM and install Docker | Pat | S | |
| 3.2 | Create the production secrets and settings | Pat | S | Newly generated values, never the local ones. Claude can't handle the real ones. |
| 3.3 | **new** Boot the full production overlay for the first time | Both | M | It has never started, and the stack has never run on Linux. Expect some fixes. |
| 3.4 | Create the tunnel, with no inbound ports | Pat | S | Target `http://app:8080`, never 8081. |
| 3.5 | Set up provider backups, plus **new** a nightly `pg_dump` and an off-provider copy of dumps and photos | Both | M | The research recommends this, because provider backups are crash-consistent at best. |

## Phase 4: Prove it, unannounced

All from section 5 of the checklist, run against the deployment.

| # | Item | Who |
|---|---|---|
| 4.1 | Real login and sign-up through the real widget | Both |
| 4.2 | Edge runbook checks, including that the origin refuses direct connections | Both |
| 4.3 | Session cookie is `Secure`, and forged client addresses are ignored (rewrite that check first) | Claude |
| 4.4 | Metrics are refused through the tunnel | Claude |
| 4.5 | One real identification | Pat |
| 4.6 | Real email: verification link works, and alerts arrive | Pat |
| 4.7 | Heartbeat check alerts when Alertmanager is stopped | Both |
| 4.8 | Credential rotation rehearsal, then rotate | Pat |
| 4.9 | Restore a database backup | Both |

## Phase 5: After launch

| # | Item | Size | Note |
|---|---|---|---|
| 5.1 | Migration tool | M | Decision B. Blocks the first schema change. |
| 5.2 | Tune alert thresholds against real traffic | S | Most fire on any single violation, and `ChallengeFailureSurge` at 20 is a guess. |
| 5.3 | Pre-filter candidate species from the catalog by area | M | Location capture, storage and the prompt context shipped with 1.1 (the observation's `area` cell). Still to test on a real phone. |
| 5.4 | **new** Real password reset, ending the account's live sessions on reset | L | Decision A. Must ship before the site opens beyond the invited group (decision C). `/reset/**` has no controller yet. Reuses the session ending built in 1.9: call `AccountSessions.endAll(email)` after a successful reset. |

**Not planned unless wanted:** Grafana, an admin metrics page, automated tests for `db-init`, a
`--web.external-url` for Alertmanager's email links, and an alert for the local
`VisionConfiguration` provider violation.

## Decisions for Pat

**A. Account recovery.** ✅ Decided 2026-11-25: the cheaper option now (1.5, size S), and real password reset (5.4) before opening up beyond the invited group. Recommended was: build real password reset before launch. It costs several
sessions, but with open registration, every forgotten password would otherwise be a manual job. The
cheaper option is confirm-page wording plus purging accounts never verified after 24 hours. It closes
S1 in under a session, but forgotten passwords stay unrecoverable, which only suits a small invited
group.

**B. Migration tool timing.** ✅ Decided 2026-11-25, as recommended: straight after launch (5.1). Recommended was: straight after launch, before any schema change. Adding it
before launch is safer but delays launch, and nothing in this plan changes the schema before then.

**C. Launch size.** ✅ Decided 2026-11-25, as recommended: a small invited group first, then open up. 2.6 is still open. Recommended was: start with a small invited group, then open up. It limits the damage
from anything phase 4 misses. It also decides A, because the cheaper option is only reasonable for a
small group, and it may change 2.6.

**D. Sharing the image.** ✅ Decided 2026-11-25, as recommended: separate from launch. Recommended was: treat it as separate from launch. It needs 1.2 and 1.4 before
anything is published.
