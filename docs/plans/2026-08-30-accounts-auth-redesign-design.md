# Accounts / Authentication & Authorization Redesign

- **Date:** 2026-08-30
- **Status:** Design — approved in brainstorming, pending spec review
- **Type:** Architectural (new bounded context + security-model change)

## Context

The app is being prepared for public hosting with vision-based identification
(the MVP feature). The current auth model does not fit a public deployment:

- Authentication is **authenticate-by-default** (`anyRequest().authenticated()`)
  — anonymous users cannot browse the catalog at all.
- There is **no runtime registration path**; naturalists are seeded, not created.
- Login identity is the `NaturalistName` slug — a **public, self-identifying,
  cross-domain-referenced** value (`FieldObservation.observedBy`, usage counters,
  exports). Credentials must not be keyed to the public persona.
- Vision access is gated only by *authentication* (any logged-in naturalist) plus
  the usage budget/rate limiter — there is no notion of an account being *entitled*
  to use the paid vision feature.
- Auth state (`NaturalistCredential`) lives inside the ecological `naturalists`
  domain, which its own CLAUDE.md says it does not want ("a naturalist is an actor,
  not an account").

### Goals

1. **Anonymous browsing** of the catalog.
2. **Public self-registration** with email verification.
3. **Vision access as an explicit, policy-toggled entitlement** — granted either by
   email verification (self-serve) or by admin approval (request process).
4. **Credentials decoupled from the public handle**, which is itself **renameable**.
5. **Production-ready** posture (password reset, ban switch, deliverable email).
6. **Seam for a future first-party external web app** — an addable token-based
   `/api/**` surface — without building it now.
7. **Young naturalists (future — postponed).** A teacher can register normally and
   provision **email-less student accounts** (a class of middle-schoolers) via
   classroom self-join, with student vision funded by a teacher-owned budget pool, and
   child-data collection minimized. Designed and captured, but not on the near-term
   build path — see the POSTPONED banner in the young-naturalists section.

### Non-goals (explicitly deferred)

- Building the `/api/**` bearer-token chain (first-party frontend is "later").
- Third-party OAuth2 authorization server / API-key management.
- Merging the `usage` domain into `accounts` (see Decision D2).
- The premium/credit "bypass free limits" entitlement (the existing
  `usage.EntitlementLookup`) — `accounts` becomes its natural owner later, but it
  stays a no-op for now.
- Mapping the ecological `NaturalistRole` to security authorities.

## Decisions

| # | Decision |
|---|----------|
| D1 | Vision access is **one capability, policy-toggled**. Self-registration auto-grants it on email verification; the request process grants it by admin approval. Same structure; only *when* it is granted differs, chosen by config. |
| D2 | **Accounts and usage stay separate domains.** `accounts` owns identity + credentials + the access entitlement; `usage` remains the metering engine. They collaborate through the existing `usage.EntitlementLookup` port (owned/implemented later, not in this effort). |
| D3 | **Two opaque identity keys**, both `EntityName` slugs: `AccountName` (the auth/credential identity, owned by `accounts`) and `NaturalistName` (the cross-domain naturalist handle, owned by `naturalists`). Login is by **email**, decoupled from both. No separate surrogate `AccountId` at the domain layer — the RDBMS adapter carries a private numeric PK (as `InsectSpecies` does). |
| D4 | **`naturalists` owns the public handle and the link to auth.** `Naturalist` holds an `AccountName` reference; the public handle is a **renameable** component, distinct from the stable `NaturalistName` key. |
| D5 | **Auth stays pure.** `accounts` knows nothing about naturalists or public handles. |
| D6 | External app is **first-party, later** — design the `/api/**` seam, build session-based console now. |
| D7 | **Teacher is an admin-granted account capability** (`ROLE_TEACHER`), distinct from the ecological `NaturalistRole.TEACHER`. It authorizes creating and managing classrooms + student accounts. |
| D8 | **Students are email-less accounts provisioned by classroom self-join.** A teacher-owned `Classroom` has a rotatable join code; students join with the code and pick a handle + password. The teacher's code (not email verification) is the trust anchor. Login generalizes to `loginName` = email (adults) or class-scoped username (students); `email` becomes nullable. |
| D9 | **Student vision draws on a teacher-owned classroom budget pool.** Joining a classroom grants a student `VISION` (a third grant path beside self-serve and admin-request), metered against a **classroom-scoped** `usage` counter the teacher owns and sees. |

## Identity model

Three references, all ADR-022-legal (cross-domain references are `EntityName`
slugs; surrogate `EntityId`s never cross a domain boundary):

- **`AccountName`** (opaque `EntityName`, in `identifiers`) — the stable auth
  identity `accounts` provides. Referenced by `naturalists`.
- **`NaturalistName`** (opaque `EntityName`, **moved to `identifiers`**) — the
  stable cross-domain handle for a naturalist. Unchanged in role
  (`FieldObservation.observedBy`, usage counters key on it); now opaque and
  **immutable** (it is a `NamedEntity` key).
- **`publicHandle`** (plain component on `Naturalist`) — the human "known-as" name,
  **renameable**. Renaming touches only this; the stable key and every reference
  are untouched, so historical observations never orphan.

`accounts` mints `AccountName`. `naturalists` mints `NaturalistName` and maintains
the `NaturalistName ↔ AccountName` mapping. There is no domain-level `Account`
surrogate id; the RDBMS adapter holds a private numeric PK. (`EmailVerificationToken`
is an `Entity` and does carry its own `EmailVerificationTokenId`, which stays inside
`accounts` and never crosses a boundary.)

## Domain model

### `accounts` (new domain — pure auth)

- **`Account`** — `NamedEntity<AccountName>`, mutable via explicit `with*` (verify
  email, grant/revoke vision, reset password, suspend/reinstate). Components:
  `AccountName name`, `String loginName` (`@UniqueValue` — email for adults, a
  class-scoped username for students), `@Nullable String email` (contact for
  verify/reset; null for students), `String passwordHash` (bcrypt, never plaintext),
  `boolean emailVerified`, `AccessLevel access`, `AccountStatus status`,
  `AccountKind kind`, `boolean canProvisionStudents` (the teacher capability),
  `@Nullable AccountName sponsoredBy`, `@Nullable ClassroomName classroom`. Owns
  nothing, so it is a `NamedEntity`, not an `Aggregate`.
- **`AccountKind { SELF_REGISTERED, STUDENT }`** — selects the login/verification
  model (email + verify vs. username + classroom sponsorship).
- **`Classroom`** — `NamedEntity<ClassroomName>` (opaque key), mutable via `with*`.
  Components: `ClassroomName name`, `String label`, `AccountName teacher` (owner),
  `String joinCode` (`@UniqueValue`, rotatable), `ClassroomStatus status`
  (`OPEN`/`CLOSED` for joining), `int maxSize`, `boolean requiresApproval`. The
  vision budget pool is a **classroom-scoped `usage` counter** the teacher configures
  (not stored on `Classroom` — `usage` owns limits).
- **`AccessLevel { BROWSE_ONLY, VISION }`** — the access entitlement.
- **`AccountStatus { ACTIVE, SUSPENDED }`** — `SUSPENDED` → Spring
  `accountNonLocked = false` (the abuse ban switch).
- **`EmailVerificationToken`** — `Entity<EmailVerificationTokenId>`, references the
  account by `AccountName` (intra-domain FK), holds a **hashed** token (raw token only
  ever in the emailed link), `purpose { VERIFY_EMAIL, RESET_PASSWORD }`, `expiresAt`,
  `consumedAt`. One type serves verify + reset.
- Ports: `AccountQuery` (`byEmail`, `byName`), `AccountCommand` (`register`,
  `verifyEmail`, `grantVision`, `revokeVision`, `beginPasswordReset`,
  `resetPassword`, `suspend`/`reinstate`), repositories, plus token query/command.
- Standard stack: `accounts-api`, `accounts-core`, `accounts-repository-test`,
  `accounts-repository-rdbms`.

### `naturalists` (changes — owns identity + link + handle)

- `Naturalist` keyed by the stable `NaturalistName`, gaining `AccountName account`
  (link to auth) and a **renameable** `publicHandle` (`@UniqueValue`, to block
  impersonation — relaxable). Ecological `role`/`stage` unchanged.
- **`NaturalistCredential` and `NaturalistCredentialQuery` are deleted** — auth
  state moves into `Account`.
- Gains its **first write path**: `NaturalistCommand.create(naturalistName,
  accountName, publicHandle, role, stage)` and `rename(naturalistName, newHandle)`.
- `NaturalistName` moves to `identifiers`.

### Registration orchestration (app layer)

Lives in `management-console` (`RegistrationService`): mint via
`accountCommand.register(email, encoder.encode(password))` → get `AccountName` →
`naturalistCommand.create(mintedNaturalistName, accountName, publicHandle, VISITOR,
stage)`. Two domains, composed at the root — no cross-domain core dependency.

## Authorization & security chain

`SecurityConfiguration` (the only Spring-Security-aware module) inverts the default
to **anonymous-read**:

| Route | Rule |
|-------|------|
| `/`, `/css/**`, `/js/**`, `/images/**` | permitAll |
| `/register`, `/verify`, `/login`, `/reset/**`, `/join/**` | permitAll (join is code-gated) |
| `GET /insects/**`, `GET /plants/**`, `/citations`, clade/rank pages | permitAll |
| `POST /insects/identify`, `POST /plants/identify` | `hasAuthority("VISION")` |
| other member `POST`s (`/observe`, `/{name}/images`, `/{name}/notes`) | `authenticated()` |
| `/naturalists/me/**` (profile, change handle) | `authenticated()` |
| `/teacher/**` (classrooms, student roster, class budget) | `hasRole("TEACHER")` |
| `/admin/**` | `hasRole("ADMIN")` |
| `anyRequest()` | `authenticated()` |

- **Anonymous browse** needs almost no console change: the console already resolves
  the current naturalist as an `Optional` (`me.isEmpty()` → collection-lens off,
  images owner-less). `NaturalistHeaderInterceptor` publishes no naturalist
  attribute when unauthenticated.
- **Unverified users can log in and browse** as `ROLE_NATURALIST`; they lack
  `VISION` until verify (self-serve) or admin grant (request). `emailVerified`
  gates the *authority*, not login.
- **Authorities:** config admin → `ROLE_ADMIN`; naturalist → `ROLE_NATURALIST`, plus
  `VISION` iff `Account.access == VISION`, plus `TEACHER` iff
  `Account.canProvisionStudents`, all derived at login. `SUSPENDED` →
  `accountNonLocked=false`. The ecological `NaturalistRole` (including its `TEACHER`
  and `STUDENT` values) is **not** an authority — the `TEACHER` *authority* is the
  admin-granted account capability, a separate axis.
- **Vision gate at the edge:** the `hasAuthority("VISION")` matcher rejects before
  the controller; the controller keeps its own `me.isEmpty()` check as
  defense-in-depth.
- **CSRF stays on** (session + forms), using the existing request-attribute token
  the rank pages already consume.
- **Deferred external-app seam:** the console rules live in a `securityMatcher`-scoped
  chain so a second `@Order`-ed, **stateless `/api/**` bearer-token chain** is
  additive later — no surgery on the session chain.

The identify call thus has three cleanly-separated, separately-owned gates:
**accounts** (authority: "may they?") → **resilience** (rate limit) → **usage**
(budget: "capacity left?").

## Account lifecycle flows

- **Register** (`POST /register`, public): create `Account` (`emailVerified=false`,
  `access=BROWSE_ONLY`, `status=ACTIVE`) + `Naturalist`; mint a `VERIFY_EMAIL` token
  (hashed, ~24h), email the raw-token link; **auto-login** the session as
  `ROLE_NATURALIST` (no `VISION`) → browse immediately.
- **Verify** (`GET /verify?token=…`, public): hash → find unconsumed/unexpired →
  `verifyEmail` sets `emailVerified=true`, and **if policy=self-serve also sets
  `access=VISION`**; request-mode leaves `BROWSE_ONLY`. Consume token; re-derive the
  session authorities so `VISION` takes effect without re-login.
- **Request-mode grant** (admin): `/admin/accounts` lists verified-but-`BROWSE_ONLY`
  accounts (the implicit pending queue); `POST …/grant-vision` → `grantVision`.
  Revoke is the inverse. Present in both modes (self-serve uses it to revoke/ban).
- **Password reset** (public): `POST /reset/request` responds identically regardless
  of whether the email exists (no account enumeration); mints a `RESET_PASSWORD`
  token (~1h) when it does. `POST /reset` (token + new password) → `resetPassword`.
  Resends rate-limited (resilience limiter) against email-bombing.
- **Change handle** (`POST /naturalists/me/handle`, authenticated):
  `naturalistCommand.rename(naturalistName, newHandle)`. Stable key unchanged.

## Email infrastructure

- One `EmailSender` port. **Default profile is a dev logging sender** that prints the
  verify/reset link — the whole flow works end-to-end with **no SMTP server**.
- Production wires a **transactional-email provider as an SMTP relay** (Resend /
  Postmark / Brevo / Amazon SES) into the `spring.mail` block (currently commented;
  `JavaMailSender` is a no-op until active). SMTP relay = **no code change**, just
  config. An HTTP-API provider would instead get an alternate `EmailSender` adapter.
- **Email provider is a deploy-time concern, not a code dependency.** The real work
  is deliverability, not code: a sending domain + SPF/DKIM (DMARC) DNS records, and
  (for SES) a one-time sandbox→production approval. Constraint captured for deploy:
  **provider TBD; must be SMTP-relay-compatible.**

## Policy toggle

`naturalist.access.grant-policy: self-serve | request` (default `self-serve`). Read
in the verify path (D1). The admin grant/revoke UI is present in both modes.

## Young naturalists (classrooms & students)

> **Status: POSTPONED.** Designed and captured here, but **not on the near-term
> build path** (slices 7–9 below are deferred). Gated on a COPPA/child-privacy
> review before any minor is onboarded. Also noted in
> [`domains/naturalists/README.md`](../../domains/naturalists/README.md).

The domain already anticipated education: `NaturalistRole` has `TEACHER`/`STUDENT`
and `EcologicalStage` is the Durrell learning progression. This adds the
*provisioning + access* layer on top.

- **Teacher capability.** An admin grants `Account.canProvisionStudents`
  (`accountCommand.grantTeacher`), surfacing as the `TEACHER` authority. A teacher
  registers and verifies like any adult; the capability is orthogonal to their
  vision access.
- **Classroom.** A teacher creates a `Classroom` (`/teacher/classrooms`) with a label;
  the system issues a **join code**. The teacher can **rotate** the code, **close**
  joining, set **`maxSize`**, and (optionally) require **approval** so joins land in a
  pending roster before activating. These are the safety rails: a leaked code cannot
  mint unbounded vision-spending accounts.
- **Student self-join** (`/join?code=…`, public). The student enters a **handle +
  password**; the system mints an `Account` (`kind=STUDENT`, `email=null`,
  `loginName` = class-scoped username, `sponsoredBy` = teacher, `classroom` set,
  `access=VISION`) + a `Naturalist` (`STUDENT`, early `EcologicalStage`). No email,
  no verification — the code + teacher sponsorship is the trust anchor. This is the
  **third grant path** for `VISION`, beside self-serve verification and admin request.
- **Student login.** `loginName` (class-scoped username) + password. Students reach
  login through their classroom (code or class link); the stored `loginName` is
  globally unique via composition so "ada" can exist in two classes.
- **Classroom budget pool.** The teacher owns a **classroom-scoped `usage`** vision
  budget; every student `reserve(NaturalistName)` also draws the classroom counter,
  and rejects when the pool is exhausted (independent of, and beneath, the global
  ceiling). The teacher sees classroom usage. This extends `usage` with a classroom
  scope (`UsageScope`) keyed by `ClassroomName` — **coordinate with the in-flight
  `feature/usage-event-log` branch**, which is also editing `usage`.
- **Child-data minimization (COPPA-aware — not legal advice).** Students supply **no
  email and minimal PII** (a handle, optionally a first name). The teacher/school is
  the consenting adult. Student accounts have no password-reset-by-email (the teacher
  resets), no public profile beyond the handle, and are bulk-deactivatable at year
  end. A real COPPA/privacy review is an **open item** before onboarding minors.
- **Lifecycle.** The teacher can reset a student's password, remove a student, and
  close/archive a classroom (deactivating its students).

`ClassroomName` (opaque `EntityName`) lives in `identifiers` — referenced by both
`accounts` and `usage` (the classroom-scoped counter). `AccountKind` and
`ClassroomStatus` are `accounts` enums.

## Testing strategy

- `accounts` uses the standard stack: `TestEntitySource` + JSON for `Account` and
  `EmailVerificationToken`; `EntityRepositoryTest` contracts run against **both** mock
  and rdbms adapters (ADR-002 three-case: arg validation, empty, expected). Declare
  the `EmailVerificationToken → AccountName` FK in `foreignKeyConstraints()` (mock/rdbms
  FK-parity — Postgres write-ordering).
- Security: `@WebMvcTest` slice tests in `management-console` via `ConsoleSliceTemplates`
  — anonymous GET permitted, identify requires `VISION`, member POST requires auth,
  `/teacher` requires TEACHER, `/admin` requires ADMIN, register/verify/reset/join public.
- Flows: token hashing/expiry/single-use; both policy modes; no-enumeration on reset;
  rename leaves stable `NaturalistName` + observations intact.
- Classrooms: join-code gate (closed/rotated/expired code rejected), `maxSize` cap,
  optional approval roster, student mint (no email, `access=VISION`), and the
  classroom budget pool rejecting a student `reserve` when exhausted while a second
  classroom is unaffected (scope isolation).
- Standard gates: `mvn verify` + `mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`;
  N+1 no-fan-out gate; standing-DB `usage_event`-empty invariant preserved.

## Migration

- `NaturalistName` is **already in `identifiers`** (`com.naturalist.naturalist.NaturalistName`,
  physically in the `identifiers` module; ~61 references already import it from there,
  no copy in `naturalists-api`). **No move is needed** — slice 1 as originally framed is
  already done. The stale `naturalists` CLAUDE.md/README that say it lives in the domain
  should be corrected. `identifiers` only gains the genuinely new opaque names
  `AccountName` and (postponed) `ClassroomName`.
- Delete `NaturalistCredential`/`NaturalistCredentialQuery`; repoint
  `NaturalistUserDetailsService` at `AccountQuery`. Existing credential seed data
  migrates to `Account` rows; each seeded naturalist gets a backfilled `publicHandle`
  (old display) + minted `AccountName`.
- RDBMS: new `account` (private numeric PK + unique `account_name`, `email`) +
  `email_verification_token` (FK → `account_name`) tables; `naturalist` gains
  `account_name` + `public_handle`; `naturalist_credential` table retired. Standing
  test-DB reseed via `apps/test-db-seeder`; checksum-diff to detect drift.

## Phasing (dependency-ordered, ≤400-line PRs per ADR-019)

1. ~~Move `NaturalistName` → `identifiers`~~ **Already done** — `NaturalistName` is
   already in `identifiers`. Reduced to a docs fix (correct the stale `naturalists`
   CLAUDE.md/README) + add the new `AccountName` identifier.
2. `accounts` domain, identity only (records, ports, mock + rdbms + contracts, TES + JSON).
3. `naturalists`: add `account` link + `publicHandle` + create/rename commands; delete
   credential; repoint `UserDetailsService`; seed migration. **Login still works.**
4. Security-chain inversion + anonymous browsing + `VISION` authority (slice tests).
5. Registration + verify + `EmailSender` port + dev logging sender + SMTP config +
   policy toggle (self-serve end-to-end).
6. Password reset + change-handle + admin grant UI (request-mode).
Slices 1–6 are the near-term build path.

**Postponed (young naturalists — see the POSTPONED banner above; build only when
scheduled, after a COPPA review):**

7. **Teacher capability**: `Account.canProvisionStudents` + admin grant UI + `TEACHER`
   authority + `/teacher` chain.
8. **Classroom + student self-join**: `Classroom` entity/ports/repo/tests; `/join`
   flow; `loginName` generalization (email-or-username, nullable email); student mint;
   teacher classroom UI (create, rotate/close code, roster).
9. **Classroom vision budget**: `usage` classroom scope + pool counter + teacher
   budget UI. *Sequence after `feature/usage-event-log` merges* (both edit `usage`).

**Deferred (separate future effort):**

10. `/api/**` stateless bearer-token chain (first-party external app).

Slices 1–6 don't touch `usage` internals (only slice 1's rename brushes its imports —
an easy rebase). The postponed slice 9 is the one real `usage` change and is sequenced
after the in-flight `feature/usage-event-log` branch lands.

**Note on the model surface:** because young naturalists are postponed, the
`Account` fields that exist *only* for them — `AccountKind`, `canProvisionStudents`,
`sponsoredBy`, `classroom`, and the `loginName`/nullable-`email` generalization — do
**not** need to land in slices 2–3. Build the adult `Account` with `email` as the
`@UniqueValue` login and add the student-shaped fields when slice 8 is scheduled. The
design records the end state; the near-term build carries only the adult shape.

## Open items

- **Email provider** — chosen at deploy; must be SMTP-relay-compatible.
- **Hosting platform** — undecided; informs the eventual `/api/**` chain and (from the
  prior thread) the option to retire the Anthropic API key via Workload Identity
  Federation. Out of scope here.
- **COPPA / child-privacy review** — required before onboarding minors. This design
  minimizes collection (no student email, minimal PII, teacher/school as consenting
  adult), but a real privacy/legal review of the student flow is a prerequisite, not a
  code task.
- **Per-student sub-caps within a classroom pool** — deferred; the teacher-owned pool
  is the MVP envelope. Add teacher-configurable per-student limits later if needed.
- **Deferred grant path for students** — joining grants `VISION` immediately; if abuse
  appears, add a teacher-approval step before `access=VISION` (the `requiresApproval`
  roster already models the hook).
