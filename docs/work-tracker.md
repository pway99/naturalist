# Work Tracker

Dashboard for what's in flight. Each row points to its source-of-truth doc; this file owns no scope.
Forks and open questions live in the [parking lot](notes/parking-lot.md). Launch detail lives in the
[public launch work plan](plans/2026-11-05-public-launch-work-plan.md) and the
[hosting readiness checklist](hosting-readiness.md). [`plans/README.md`](plans/README.md) indexes plan files.

Last updated: 2026-11-25 (after PR #17), rebuilt from a roll-call of every session active since
2026-11-01 and checked against git. `origin/main` = `d2947203`; the main checkout's local `main` is
at `760799fc` and needs a pull. Everything shipped before 2026-11-12 is in the
[2026-11-12 snapshot](plans/archive/work-tracker-2026-11-12-snapshot.md).

---

## Resume order

The goal is the public launch at `backyardnaturalist.app` (MVP: hosted vision identification). Launch
work comes first; the paused engineering threads follow.

| #  | Task | Who | Size | Why now |
|----|------|-----|------|---------|
| 1  | ✅ 2026-11-25 **Launch decisions A–D and 2.3 host**: C small invited group first; A the cheap option now (confirm-page wording + purge accounts unverified after 24 h), real reset (launch 5.4) before opening up; B migration tool straight after launch; D image sharing separate; host DigitalOcean 4 GiB x86, US | Pat | S | Done |
| 2  | ✅ 2026-11-25 **Launch 1.5: account recovery, cheap option** (closes security finding S1): confirm-page wording; hourly purge of registrations never verified once their 24 h link expired (tokens, account, naturalist); member writes now need a verified email (`VERIFIED` authority) | Claude | S | Merged to main 2026-11-25 (`26eca2b0`). Real password reset is launch 5.4, before opening up |
| 3  | ✅ 2026-11-25 **Launch 1.9: admin accounts page** (`/admin/accounts`: paged list; grant/revoke `VISION`; suspend/reinstate; suspend ends live sessions via `AccountSessions`) | Claude | M | Merged to main 2026-11-25 (`9cf8992c`). `request` is now workable for 2.6; 5.4 reuses `AccountSessions.endAll` |
| 4  | ✅ **Java4Shell WI-2 + rest of WI-6** (on main 2026-11-25): runtime pinned to `eclipse-temurin:25.0.5.1_2-jre-noble` (Adoptium's named CVE-2026-114938 fix), WI-1 flag removed, actions pinned by SHA, `dependabot.yml` (docker, docker-compose, github-actions, maven), plus `.github/workflows/image.yml`, which builds both images on every PR that touches them. The weekly `--pull` rebuild is deferred to the production redeploy job | Claude | S | Done; deploy the pinned image when the DigitalOcean VM exists (launch 3.1) |
| 4a | ✅ 2026-11-25 **Dependabot's first run triaged**: #6 actions and #7 compose images merged; #5 (Temurin 26) closed, and the config now ignores Temurin majors and the maven build image; PR #12 supersedes #8, #10, #11 (Spring Boot 4.2.0 with every Boot-managed property re-pinned and two deprecated `server.*` keys renamed; anthropic-java 3.12.0 with the v3 builder migration; MyBatis 3.6.0, CDK 2.14.1, ArchUnit 1.5.1). `.sdkmanrc` pins Maven 3.9.16 | Claude | S | Merged to main (`39e87de8`). #9 build plugins waits, see the small backlog |
| 5  | ✅ 2026-11-25 **Launch 1.8: storage-capacity alerts**: Boot's disk-space metrics for `/data/images` and `/data/database` (read-only mount, no exporter); `DiskSpaceLow`, `DiskFillsWithinThreeDays`, `DiskSpaceCritical`, `PhotoDeleteFailed`, all promtool-tested | Claude | S–M | Merged to main 2026-11-25 (PR #13, `d255a712`). Thresholds tuned in launch phase 5 |
| 6  | ✅ 2026-11-25 **Seed data review**: ✅ 2026-11-25 launch 1.2 seed field observations (places → "Sacramento Valley, CA", note typos fixed, observer slugs kept); ✅ 2026-11-25 the "Oak Vista" / "Chico" sweep (193 files: prose → "the garden" and "Sacramento Valley, CA", lineage slugs → `*-garden-saved`, emails → `@naturalist.example`, chemistry keys → `local*` / `garden*`; cold storage left as history); ✅ 2026-11-25 Java4Shell WI-4 throwaway seed passwords (none is real, and production loads no seed accounts) | Pat | S–M | Public pages show this content. Git history still holds the old strings. Standing test DB reseeded 2026-11-25 from the sweep plus launch 1.2's field observations |
| 7  | ✅ 2026-11-25 **Java4Shell WI-3: egress allowlist proxy** + ADR-029 (`app`/`db`/`db-init` on an internal network; Squid `egress-proxy` allows only the model API, the challenge endpoint and the mail host) | Claude | M | Merged to main 2026-11-25 (PR #17, `d2947203`); `image.yml`'s allowlist probe passed on GitHub. VM checks (Docker Engine ≥ 26, `deploy/egress-proxy/check.sh`) are in hosting-readiness §5; DigitalOcean's SMTP block bears on 2.4; four fix-forward minors in the small backlog |
| 7a | **Pre-launch, no account needed: production rehearsal**: boot `compose.yaml` + `compose.production.yaml` locally for the first time (launch 3.3: throwaway secrets, emulated x86, the egress proxy's production config), then the nightly `pg_dump` + photo-copy scripts and a restore rehearsed locally (3.5, 4.9). Only the off-provider storage target waits for an account | Claude | M | The overlay has never started and the stack has never run on Linux; find that before the VM, not on it |
| 7b | **Pre-launch, no account needed: small fixes**: the egress-proxy fix-forwards and the ADR-028 minors (small backlog); a test per Anthropic adapter pinning the v3 request shape and `Authorization: Bearer` header; rewrite the forged-address check to carry a browser's clearance (prep for 4.3); `cache: maven` on CI's `setup-java` (Maven Central 429s, 2026-11-26) | Claude | S each | Cheap now; each one otherwise surfaces in phase 4 |
| 8  | **Launch phases 2–3: outside accounts and production** (vendor workspace + spend limit, Cloudflare zone, email + SPF/DKIM, heartbeat, DigitalOcean VM, secrets, tunnel, backups) | Pat (+ Claude for 3.3, 3.5) | M–L | Host decided; phase 3 needs 2.2, 2.4 and 2.5. **Paused 2026-11-26:** Pat is not ready to open the DigitalOcean account and returns to application work first |
| 9  | **Launch phase 4: prove it unannounced** (real widget, edge checks, Secure cookie, real identification, email, rotation rehearsal, restore) | Both | M | Needs #8 |
| 10 | **Named values step 3**: ~~plants (PR #19, merged 2026-11-27)~~ → insects edge levels to the B1 INFO/WARN split → chemistry 4 → garden 3 → soil / glossary / concepts / registration 1 each; then `TypedPathVariable` joins `EnforceArchitecture` | Claude | M | Edge rule moved into the standards 2026-11-27 (B1, M1) |
| 11 | **Observability: resume the sweep**: path-mode reviews (plants, insects-console); slices 4–8, plus the 5 catch sites PL-19 unblocks | Pat → Claude | L | Slices 1–3 on main (PR #3 merged 2026-11-25); on hold until Pat finishes reviewing the rules |
| 12 | **Small backlog** (see the table below) | Claude | S each | Fill-in work |

## Active efforts

| Effort | State | Where | Left |
|--------|-------|-------|------|
| Public launch ([plan](plans/2026-11-05-public-launch-work-plan.md), [checklist](hosting-readiness.md)) | In progress | main | Phase 0 ✅. Phase 1 ✅ (1.1–1.9; 1.2 merged as PR #15). Phase 2: 2.3 ✅ (DigitalOcean). Phases 3–4 not started. Pat also owes a `backyardnaturalist.app` address in `secrets/alert-email` on the host |
| Java4Shell hardening ([plan](plans/2026-11-19-java4shell-hardening-plan.md)) | In progress | WI-1 `f8d9de23` | WI-1 ✅, then retired by WI-2. WI-5 ✅ (`62f05c18` SECURITY.md; private reporting, Dependabot alerts, CodeQL on). WI-2 ✅ (pinned Temurin 25.0.5.1+2, the confirmed fix; WI-1 flag retired; `image.yml` builds images on PRs; scheduled rebuild deferred to production). WI-6 ✅ (read-only token `9e27d97f`; actions pinned by SHA; Dependabot config). WI-7 ✅ ("main safety net" ruleset). WI-4 ✅ (2026-11-25: Pat confirmed no seed password is real; `DeploymentDbInitializer` loads no accounts). WI-3 ✅ (2026-11-25, ADR-029: egress allowlist proxy, PR #17 `d2947203`). **WI-8 open.** On the DigitalOcean VM once it exists: deploy the pinned image, install Docker Engine ≥ 26, run `deploy/egress-proxy/check.sh` |
| Cloudflare edge ([ADR-027](adr/ADR-027-cloudflare-edge-and-abuse-protection.md)) | Code done | PRs 1–7 on main | Real-widget login check and deployment (launch 4.1, 4.2) |
| Secrets + model spend ([ADR-028](adr/ADR-028-secrets-and-model-spend-protection.md)) | Code done | main | Operational only: step 0 vendor workspaces/limits/keys (launch 2.1), step 7 rotation rehearsal (4.8), step 8 deploy config + forged-address check + refusal alert rule (4.3). Turnstile secret is on `SecretProvider` ✅ |
| Named-value serialization ([design](plans/2026-11-18-named-value-serialization-design.md)) | Paused 2026-11-22 | steps 1–2 + insects step 3 on main | Step 3 for 7 more controllers, then arm `TypedPathVariable` |
| Observability standards + reviewer ([standards](observability-standards.md)) | Paused | main | Next path-mode chunks: plants, insects-console. Optional Sonnet trial of the reviewer |
| Observe-every-throw sweep ([plan](plans/2026-11-14-observe-every-throw-plan.md)) | On hold since 2026-11-15 | slices 1–3 on main (PR #2, PR #3 merged 2026-11-25) | Re-check slices 1–3 against the standards; slices 4–8 |
| Security review follow-ups ([notes](notes/2026-11-03-security-review.md)) | Open | — | S1 ✅ closed by launch 1.5 (2026-11-25, `26eca2b0`); S2 is checked at launch 4.3 |

## Waiting on Pat

**Launch:** 2.1 spend limit (proposed $60/month), whether dev gets its own limit, and identify in
Cloudflare rule E1 (ADR-027 recommends no); 2.4 email provider (DigitalOcean blocks outbound SMTP
25/465/587 on new Droplets: pick a provider that accepts port 2525, or ask DigitalOcean to unblock
587; ADR-029); 2.5 heartbeat service; 2.6 who gets
identification at launch, reopened by decision C (`self-serve` makes "invited" only unannounced;
`request` grants are made at `/admin/accounts`, launch 1.9); a metrics backend. Decisions A–D and the 2.3 host were
settled 2026-11-25.

**Named values / observability:**
- A second `MethodObserver` per method (always-mode `refusals` in `identify` / `reserveStoreAndCatalogue`): name it or drop it.
- Group A request records for `InsectPhotoCommandImpl.add` / `InsectIdentificationCommandImpl.identify`.
- The deferred `InsectsController.addImage` diff.
- Existing-code rows in `InsectIdentificationCommandImpl`: J2, M4, M8, M11.
- Existing-code rows in both Anthropic adapters (from the PR #12 review): the missing-`tool_use` throws and `messages().create` failures go uncounted (J1/J3), `buildTool` / `serializeInput` catches wrap without `noticeError` (J3), constructors use `Objects.requireNonNull` (M5).

**Small calls:**
- Is a core module's test-scope dependency on another domain's `-test-context` within "core depends only on apis"?
- The Battus genus description says "(see InsectSpecies.chemicalDefense)": drop or reword (needs a test-DB reseed).

**Housekeeping:**
- Delete the leftover `~/dev` folders from the history rewrite. `naturalist-old-history`, `naturalist-backup-2026-09-26.git` and `public/naturalist` still contain the address.
- Review GitHub installed apps, tokens and SSH keys; save recovery codes in 1Password.

## Small backlog

- ADR-028 deferred minors: `Secret.hashCode`; `NoSystemGetenv` misses method references; the `.env.*` ignore also catches `.env.example`; BUSY spends rate permits.
- Dynamic test contexts: the reachability check doesn't inherit class-level `@Transactional`; cache parsed JSON test catalogs per JVM (the dominant core-test cost).
- Traffic snapshot job: a failed run only writes a FAILED line to `~/dev/naturalist-traffic/snapshot.log`.
- Kaname engine issues #1 and #2 (kaname-evaluators), waiting on the engine team.
- `rewrite-recipe-bom` 3.38.0 is skipped in `dependabot.yml` (`4c3755e7`), because it imports `org.openrewrite:rewrite-bom:8.91.0`, which was still 404 on Maven Central. The other nine build-plugin bumps merged as PR #16 (`760799fc`). Lift the skip once 8.91.0 is published.
- Egress proxy minors (WI-3 review, fix forward): `deploy/egress-proxy/start.sh` renders `SMTP_HOST`/`SMTP_PORT` unvalidated (a space or `|` in `.env` breaks or widens the rule); the `inward` list lacks `0.0.0.0/8`, `100.64.0.0/10`, `::1`, `::ffff:0:0/96`; a mail host given as an IP address can never match (`dstdomain -n`), undocumented; `check.sh` always fails two checks locally (app also on `default`).
- The Anthropic adapters' tests check construction only; nothing pins the request shape, and the v3 `Authorization: Bearer` header has not met the live API (launch phase 4 does).

## Worktrees and branches

One worktree besides the main checkout: `.claude/worktrees/confident-cohen-d3c979` (the WI-3 session),
on `claude/wi3-tracker-merged` for this tracker update. Its earlier branch `claude/zen-lamarr-b7bfc8`
(local and `origin`) is fully merged by PR #17 and can be deleted. Before 2026-11-25's WI-3 work there
were no worktrees, only the main checkout. Cleaned up 2026-11-25: four merged worktrees and seven merged
branches removed; then the `observe-every-throw-8d0d18` worktree (reused by the Java4Shell WI-2
session) and its branch `claude/java4shell-wi2-pinning-fa05da`; the `dependabot-followups-954d84`
worktree and its branch, after PR #12 merged; the launch 1.5 and 1.9 worktrees and branches; and the
PR #3 branch `claude/observe-every-throw-8d0d18`. All were fully on `main`. Recreate a worktree when
named values resumes.

Still to delete (Pat; fully on `main`):
- Local `backup/main-before-rebase-2026-11-15`: pre-rebase copies of the photo-sizes commits, each patch-equivalent to one on main.
- The `.claude/worktrees/launch-1-5-account-recovery-c3a363` folder, which holds only an `.idea` folder IntelliJ recreated after the worktree was removed.

## Later (designed or noted, not scheduled)

- **Launch phase 5:** migration tool (blocks the first schema change), alert-threshold tuning, area pre-filter for candidate species.
- **Re-identification:** rebuild `/insects/{name}/re-identify` on `InsectCommand.sightings()` after the authorization work.
- **Accounts:** slice 6 is now split: the admin grant page is launch 1.9, password reset is launch 5.4, and change-handle is still unscheduled. Young naturalist and classroom accounts are postponed.
- **Soil↔naturalist ownership:** `Property` aggregate ([memo](notes/2026-08-09-soil-profile-naturalist-association.md)).
- **Plants:**
  - Info pattern (`PlantTaxonView`, `OrganismFeatureView`, `CladeView`).
  - Child-card thumbnails and role badges.
  - Unused `FeatureCollection`.
  - Plant `findByName` ([design](plans/2026-11-26-plant-find-by-name-design.md)): binomial strategy (a missing species answers its genus, the species page redirects there with a "not added yet" note) built on `claude/named-values-step3-cbc835`; `getByName` stays exact. Next PR: a look-alike search strategy behind the same `findByName`. Open: whether the redirect is also counted at the edge (B1).
- **Catalog and navigation:**
  - Dual-strategy slice 3, the collection lens.
  - `/concepts` restyle.
  - Soil back-references on element pages.
  - Taxonomic-scope breadcrumb primitive.
- **Older plans still open:** catalog kernel M9b–M12, command framework follow-ups, admin console view 4, the *Battus philenor* pressure test (paused), soil/sensor backlog ([notes](notes/pending-implementation.md)), insect read-model R2–R5 + R7.
- **Hexagonal adapter review** ([notes](notes/2026-11-01-adapter-hexagonal-review.md)): about 35 of 40 violations are in the consoles.
- **Feature search:** Solr/Postgres `FeatureSearch` adapter.

## Recently completed (November)

| Effort | Done | Commit(s) |
|--------|------|-----------|
| Java4Shell WI-3 (ADR-029): egress allowlist proxy; `app`/`db`/`db-init` on an internal network; Squid allows only the model API, Turnstile and the mail host; `check.sh` + `image.yml` probe | 2026-11-25 | `881e0eca` (PR #17, `d2947203`) |
| Dependabot build plugins (PR #16, supersedes #9), `rewrite-recipe-bom` 3.38.0 skipped, `-DskipTests` skips ITs again under failsafe 3.6 | 2026-11-25 | `4c3755e7`, `2e6ff901`, `760799fc` |
| Launch 1.2: seed field observation places generalized (PR #15) | 2026-11-25 | `a23f062b` (PR #15, `651873a7`) |
| Storage-capacity alerts (launch 1.8): Boot disk-space metrics for the image and database volumes; `DiskSpaceLow`, `DiskFillsWithinThreeDays`, `DiskSpaceCritical`, `PhotoDeleteFailed` | 2026-11-25 | `d255a712` |
| Dependabot first run: PR #12 (Boot 4.2.0 + Boot-managed re-pin, anthropic-java 3.12.0, MyBatis 3.6.0), #6, #7; #5/#8/#10/#11 closed | 2026-11-25 | `39e87de8` |
| Java4Shell WI-2 + WI-6: Temurin 25.0.5.1+2 pinned (WI-1 flag retired), actions pinned by SHA, Dependabot, `image.yml` | 2026-11-25 | `72247a8f`..`5b3f77c5` |
| Repo security settings: main ruleset, Dependabot alerts + fixes, CodeQL, private reporting, read-only CI token, SECURITY.md | 2026-11-23 | `9e27d97f`, `39877cb8`, `62f05c18` |
| Code-hygiene gates: `NoVar`, explicit imports, `ObserverMethodName`, IntelliJ AutoFormat | 2026-11-22 | `fa164ca2`, `808f5f38`, `4ff04270` |
| Photo sizes (launch 1.7): 1024 px photo + 640 px tile, store before charge, atomic writes | 2026-11-22 | `353df6ca` |
| Serve/cleanup observability fixes | 2026-11-19 | `3e314273` |
| Java4Shell WI-1: `-XX:-UseBASE64Intrinsics` | 2026-11-19 | `f8d9de23` |
| Observability standards + reviewer agent; image-store path review | 2026-11-18 | `861a4467`..`a90d4653`, `8dc6b59e` |
| Observe-every-throw slice 3 (PR #3): `Clade.find` / `Bioregion.find`, catalog assemblies, `RankNameDeserializer` | 2026-11-25 | `4af1d00d` |
| PL-19 `noticeError` caught-failure counter (PR #4) | 2026-11-15 | `a1a5cec6` |
| Observe-every-throw slices 1–2 (PR #2) | 2026-11-14 | `2243a37c` |
| Dynamic test contexts, all nine domains; Mockito excluded from Spring test starters | 2026-11-07 | `10665af4`, `7af7e140` |
| Insects Info pattern (`*Info` records, sealed `InsectTaxon`, `InsectCitations`) | 2026-11-02 | `551c315f`..`420604ae`, `1e3d3c42` |
| Weekly GitHub traffic snapshot (outside the repo, `~/dev/naturalist-traffic`) | 2026-11-24 | — |

---

## Conventions

- A **dashboard**, not a synthesis. Each row points to its source-of-truth doc.
- When an effort ships, move it to *Recently completed*; once it is a few weeks old, drop it (git and
  the archived snapshots keep the history).
- **Forks** go to the [parking lot](notes/parking-lot.md), not into this file.
- **Lighter slice plans for repetitive work:** a one-paragraph note, not a 300-line doc.
