# Work Tracker

Dashboard for what's in flight. Each row points to its source-of-truth doc; this file owns no scope.
Forks and open questions live in the [parking lot](notes/parking-lot.md). Launch detail lives in the
[public launch work plan](plans/2026-11-05-public-launch-work-plan.md) and the
[hosting readiness checklist](hosting-readiness.md). [`plans/README.md`](plans/README.md) indexes plan files.

Last updated: 2026-11-25, rebuilt from a roll-call of every session active since 2026-11-01 and checked
against git. `main` = `origin/main` = `62f05c18`. Everything shipped before 2026-11-12 is in the
[2026-11-12 snapshot](plans/archive/work-tracker-2026-11-12-snapshot.md).

---

## Resume order

The goal is the public launch at `backyardnaturalist.app` (MVP: hosted vision identification). Launch
work comes first; the paused engineering threads follow.

| #  | Task | Who | Size | Why now |
|----|------|-----|------|---------|
| 1  | **Decide launch decisions A and C** (account recovery approach, launch size) and **2.3 host** (DigitalOcean x86 per the research, or Hetzner; if ARM, Java4Shell doesn't apply) | Pat | S | A and C unblock 1.5; the host unblocks phase 3 and Java4Shell WI-8 |
| 2  | **Launch 1.5: account recovery** (closes security finding S1) | Claude | S or L | Last open code item in launch phase 1, and the next item in the plan |
| 3  | **Java4Shell WI-2 + rest of WI-6**: pin Temurin by full version, `dependabot.yml` (docker, maven, github-actions), pin actions by SHA, weekly `--pull` rebuild | Claude | S | `actions/*@v4` are tag-pinned and there's no Dependabot config; WI-2 also retires the WI-1 flag |
| 4  | **Launch 1.8: storage-capacity alerts** (image-store + database volume free space, failed photo deletes) | Claude | S–M | Pat asked 2026-11-16; a full disk stops everything |
| 5  | **Seed data review**: launch 1.2 seed field observations, the "Oak Vista" name (~180 files), Java4Shell WI-4 throwaway seed passwords | Pat | S–M | Public pages show this content |
| 6  | **Java4Shell WI-3: egress allowlist proxy** + ADR | Claude | M | Limits damage from any future runtime RCE; before public traffic |
| 7  | **Launch phases 2–3: outside accounts and production** (vendor workspace + spend limit, Cloudflare zone, email + SPF/DKIM, heartbeat, VM, secrets, tunnel, backups) | Pat (+ Claude for 3.3, 3.5) | M–L | Needs #1 |
| 8  | **Launch phase 4: prove it unannounced** (real widget, edge checks, Secure cookie, real identification, email, rotation rehearsal, restore) | Both | M | Needs #7 |
| 9  | **Named values step 3**: plants controller (8 markers) → chemistry 4 → garden 3 → soil / glossary / concepts / registration 1 each; then `TypedPathVariable` joins `EnforceArchitecture` | Claude | M | Paused 2026-11-22 mid-sweep; four Pat decisions listed below |
| 10 | **Observability: settle PR #3, then resume the sweep**: path-mode reviews (plants, insects-console); rework, merge or close PR #3 (slice 3); slices 4–8 | Pat → Claude | L | On hold until Pat finishes reviewing the rules; PL-19 shipped and unblocks 5 catch sites |
| 11 | **Small backlog** (see the table below) | Claude | S each | Fill-in work |

## Active efforts

| Effort | State | Where | Left |
|--------|-------|-------|------|
| Public launch ([plan](plans/2026-11-05-public-launch-work-plan.md), [checklist](hosting-readiness.md)) | In progress | main | Phase 0 ✅. Phase 1: 1.1, 1.3, 1.4, 1.6, 1.7 ✅; **1.2** (Pat), **1.5**, **1.8** open. Phases 2–4 not started. Pat also owes a `backyardnaturalist.app` address in `secrets/alert-email` on the host |
| Java4Shell hardening ([plan](plans/2026-11-19-java4shell-hardening-plan.md)) | In progress | WI-1 `f8d9de23` | WI-1 ✅ (verified on arm64 only). WI-5 ✅ (`62f05c18` SECURITY.md; private reporting, Dependabot alerts, CodeQL on). WI-6 part done (read-only token `9e27d97f`; SHA pinning and Dependabot for actions open). WI-7 ✅ ("main safety net" ruleset). **WI-2, WI-3, WI-4, WI-8 open.** Deploy WI-1 to the host once one exists |
| Cloudflare edge ([ADR-027](adr/ADR-027-cloudflare-edge-and-abuse-protection.md)) | Code done | PRs 1–7 on main | Real-widget login check and deployment (launch 4.1, 4.2) |
| Secrets + model spend ([ADR-028](adr/ADR-028-secrets-and-model-spend-protection.md)) | Code done | main | Operational only: step 0 vendor workspaces/limits/keys (launch 2.1), step 7 rotation rehearsal (4.8), step 8 deploy config + forged-address check + refusal alert rule (4.3). Turnstile secret is on `SecretProvider` ✅ |
| Named-value serialization ([design](plans/2026-11-18-named-value-serialization-design.md)) | Paused 2026-11-22 | steps 1–2 + insects step 3 on main | Step 3 for 7 more controllers, then arm `TypedPathVariable` |
| Observability standards + reviewer ([standards](observability-standards.md)) | Paused | main | Next path-mode chunks: plants, insects-console. Optional Sonnet trial of the reviewer |
| Observe-every-throw sweep ([plan](plans/2026-11-14-observe-every-throw-plan.md)) | On hold since 2026-11-15 | slices 1–2 on main (PR #2); slice 3 = PR #3 **open**, `f068ba13` | Pat decides PR #3; re-check slices 1–3 against the standards; slices 4–8 |
| Security review follow-ups ([notes](notes/2026-11-03-security-review.md)) | Open | — | S1 closes with launch 1.5; S2 is checked at launch 4.3 |

## Waiting on Pat

**Launch:** decisions A (account recovery), B (migration tool timing), C (launch size), D (sharing the
image); 2.1 spend limit (proposed $60/month), whether dev gets its own limit, and identify in Cloudflare
rule E1 (ADR-027 recommends no); 2.3 host (and whether it is ARM); 2.4 email provider; 2.5 heartbeat
service; 2.6 who gets identification at launch; a metrics backend.

**Named values / observability:**
- A second `MethodObserver` per method (always-mode `refusals` in `identify` / `reserveStoreAndCatalogue`): name it or drop it.
- Group A request records for `InsectPhotoCommandImpl.add` / `InsectIdentificationCommandImpl.identify`.
- Whether the standards gain a named-value-edge rule.
- The deferred `InsectsController.addImage` diff.
- Existing-code rows in `InsectIdentificationCommandImpl`: J2, M4, M8, M11.
- PR #3.

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

## Worktrees and branches

Cleaned up 2026-11-25: four merged worktrees and seven merged branches removed. The only worktree left
is `observe-every-throw-8d0d18`, kept because it carries open PR #3. Recreate a worktree when named
values resumes.

Still to delete (Pat; both fully on `main`):
- Local `backup/main-before-rebase-2026-11-15`: pre-rebase copies of the photo-sizes commits, each patch-equivalent to one on main.
- Local and remote `fix/serve-and-cleanup-observability`: `3e314273` is on main.
- The empty `.claude/worktrees/notes-length-18afa0` folder.

## Later (designed or noted, not scheduled)

- **Launch phase 5:** migration tool (blocks the first schema change), alert-threshold tuning, area pre-filter for candidate species.
- **Re-identification:** rebuild `/insects/{name}/re-identify` on `InsectCommand.sightings()` after the authorization work.
- **Accounts:** admin UI (slice 6). Young naturalist and classroom accounts are postponed.
- **Soil↔naturalist ownership:** `Property` aggregate ([memo](notes/2026-08-09-soil-profile-naturalist-association.md)).
- **Plants:**
  - Info pattern (`PlantTaxonView`, `OrganismFeatureView`, `CladeView`).
  - Child-card thumbnails and role badges.
  - Unused `FeatureCollection`.
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
| Repo security settings: main ruleset, Dependabot alerts + fixes, CodeQL, private reporting, read-only CI token, SECURITY.md | 2026-11-23 | `9e27d97f`, `39877cb8`, `62f05c18` |
| Code-hygiene gates: `NoVar`, explicit imports, `ObserverMethodName`, IntelliJ AutoFormat | 2026-11-22 | `fa164ca2`, `808f5f38`, `4ff04270` |
| Photo sizes (launch 1.7): 1024 px photo + 640 px tile, store before charge, atomic writes | 2026-11-22 | `353df6ca` |
| Serve/cleanup observability fixes | 2026-11-19 | `3e314273` |
| Java4Shell WI-1: `-XX:-UseBASE64Intrinsics` | 2026-11-19 | `f8d9de23` |
| Observability standards + reviewer agent; image-store path review | 2026-11-18 | `861a4467`..`a90d4653`, `8dc6b59e` |
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
