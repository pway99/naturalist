# Public launch work plan

The order of work to get the app public at `backyardnaturalist.app`. The
[hosting readiness checklist](../hosting-readiness.md) owns the detail of each item; this plan owns
the order. Items marked **new** aren't on the checklist and are owned here. Written 2026-11-05.

**Done when:** every item through phase 4 is closed, and the address is announced.

Sizes are rough guesses: **S** is under a session, **M** is about one session, **L** is several.

## Phase 0: Trust the build

| # | Item | Who | Size |
|---|---|---|---|
| 0.1 | **new** Find out why CI has never run. GitHub Actions is enabled and `ci` is active, but the repo shows 0 runs and `73a2a20b` has 0 checks. `PackagedJarIT` and the alert rule tests have never run there. | Claude | S |

## Phase 1: Fix the code

These don't need a host, and most are privacy or account problems that get harder to fix once the
site is public.

| # | Item | Who | Size | Note |
|---|---|---|---|---|
| 1.1 | Strip photo metadata on upload ([PL-17](../notes/parking-lot.md)) | Claude | S–M | Option (a): rewrite metadata only, and test that GPS is never served back. Doesn't prevent using location later (phase 5). |
| 1.2 | Review the seed field observations | Pat | S–M | They name real observers and places on public pages. Only Pat can judge what stays. |
| 1.3 | **new** Find out why 11 of 22 insect seed photos are missing | Claude | S | `db-init` reports them and still succeeds. Decide whether a missing photo should fail it. |
| 1.4 | **new** Keep the alert address private: remove it from `application.yml`, and read it from a `secrets/alert-email` file for both the app and Alertmanager | Claude | S | Replaces `ALERT_EMAIL_TO`. Use a new dedicated address, because the current one is already in public git history. Needed before sharing the image. |
| 1.5 | Account recovery (decision A) | Claude | S or L | Closes the email squatting finding, S1. |
| 1.6 | Measure a gallery-sized image read ([PL-16](../notes/parking-lot.md)) | Claude | S | Can run against the local stack. Add a bulkhead only if the numbers call for it. |

## Phase 2: Decide and set up outside accounts

These need no code and can run alongside phase 1.

| # | Item | Who | Size | Note |
|---|---|---|---|---|
| 2.1 | Model vendor: separate workspaces, spend limits, scoped credentials, retire the current one | Pat | S | Needs the spend limit decision. The plan proposes $60 a month. |
| 2.2 | Cloudflare: zone, TLS, challenge widget, security rules | Pat | S–M | [Edge runbook](../runbooks/cloudflare-edge.md), steps 1, 3 and 4. |
| 2.3 | Pick the host | Pat | S | Research points to a 4 GB DigitalOcean VM, about $29–31 a month. Report: `temp/reports/Naturalist hosting options compared.md`. |
| 2.4 | Pick the email provider, and set up the sending domain with SPF and DKIM | Pat | S | |
| 2.5 | Pick the outside heartbeat service, and create the check | Pat | S | Receives `Watchdog`. |
| 2.6 | Confirm who gets identification at launch | Pat | S | `self-serve` gives every registrant `VISION`, within the usage budgets. |

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
| 5.3 | Use photo location for identification | L | Needs 5.1, because it changes the observation record. Test on real phones first, and store only what the user confirms. |

**Not planned unless wanted:** Grafana, an admin metrics page, automated tests for `db-init`, a
`--web.external-url` for Alertmanager's email links, and an alert for the local
`VisionConfiguration` provider violation.

## Decisions for Pat

**A. Account recovery.** Recommended: build real password reset before launch. It costs several
sessions, but with open registration, every forgotten password would otherwise be a manual job. The
cheaper option is confirm-page wording plus purging accounts never verified after 24 hours. It closes
S1 in under a session, but forgotten passwords stay unrecoverable, which only suits a small invited
group.

**B. Migration tool timing.** Recommended: straight after launch, before any schema change. Adding it
before launch is safer but delays launch, and nothing in this plan changes the schema before then.

**C. Launch size.** Recommended: start with a small invited group, then open up. It limits the damage
from anything phase 4 misses. It also decides A, because the cheaper option is only reasonable for a
small group, and it may change 2.6.

**D. Sharing the image.** Recommended: treat it as separate from launch. It needs 1.2 and 1.4 before
anything is published.
