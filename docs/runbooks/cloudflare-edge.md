# Cloudflare edge

Decisions: [ADR-027](../adr/ADR-027-cloudflare-edge-and-abuse-protection.md). This runbook is how
to stand the edge up, prove it works, and respond when it fails. Zone `backyardnaturalist.app`,
Cloudflare Free plan. Dashboard labels move, so the ADR's rule table is the source of truth for
what each rule must do.

**Deploy the edge and the app together.** The app's global limits (`auth.login` 600 a minute,
`auth.register` 60 a minute) are backstops that assume the edge and the challenge are live. Never
expose the origin without them.

## Set up

### 1. Zone and TLS

Put the zone on Cloudflare DNS. Turn on Always Use HTTPS and HSTS, and set minimum TLS to 1.2 (E5).
`.app` is on the browser HSTS preload list, so plain HTTP never works for it.

### 2. Tunnel

The origin accepts no inbound traffic: every request arrives through a local `cloudflared` (D3).

1. Create a tunnel in Zero Trust under Networks, Tunnels.
2. Install `cloudflared` on the origin host, or as a sidecar, with the tunnel's token.
3. Point the public hostname `backyardnaturalist.app` at the app's port 8080: `http://app:8080` as a
   Compose sidecar, or `http://localhost:8080` on the host. Never route to 8081, the metrics port.
4. Allow no inbound 80, 443 or 8080 at the host firewall or platform ingress.

The connector must stay local or on the pod network. The app trusts `CF-Connecting-IP` only from
those peers, so a connector elsewhere makes every visitor share one rate-limit key, and a
per-client limit becomes a global lockout (`apps/CLAUDE.md`, "Client identity behind a proxy").

### 3. Challenge widget

Create one widget in Managed mode for the hostname `backyardnaturalist.app` (E6). Its settings
page shows a public site key and a credential.

Supply the credential as the secret `challenge-credential`: the environment variable
`CHALLENGE_CREDENTIAL`, or the file `<naturalist.secrets.directory>/challenge-credential`. Handle
it as `docs/runbooks/secret-rotation.md` describes, and never put it in a yml file.

### 4. Security rules

Create E1 (the one rate-limiting rule) and E2 (the custom rule) exactly as ADR-027 D2 gives them.
Turn on Bot Fight Mode (E3) and the Free Managed Ruleset (E4).

Turn Bot Fight Mode off, or scope E2 away from `/api`, when the deferred `/api/**` chain gets
non-browser clients.

### 5. App settings

Supply these at deploy time. The app refuses to boot without the challenge settings.

| Setting | Value |
|---|---|
| `naturalist.challenge.site-key` | the widget's public site key |
| `naturalist.challenge.expected-hostname` | `backyardnaturalist.app` |
| `naturalist.email.public-url` | `https://backyardnaturalist.app` |
| `CHALLENGE_CREDENTIAL` | from your secret store (step 3) |

Never set `naturalist.challenge.log-only` in a deployment, except during the incident below.

## Check after deploy

Each check should behave as described. `<origin-ip>` is the host's public address.

```bash
curl -sI https://backyardnaturalist.app/ | grep -i cf-ray
```

A `cf-ray` header proves the traffic passes through Cloudflare.

```bash
curl -m 5 http://<origin-ip>:8080/
```

```bash
curl -m 5 --resolve backyardnaturalist.app:443:<origin-ip> https://backyardnaturalist.app/
```

Both must fail to connect, which proves the origin is locked down.

```bash
curl -si https://backyardnaturalist.app/login | grep -iE '^HTTP|cf-mitigated'
```

This must show `cf-mitigated: challenge`, which proves E2. A script never reaches the form.

```bash
for i in $(seq 15); do curl -s -o /dev/null -w '%{http_code}\n' https://backyardnaturalist.app/reset/request; done
```

The status must change partway through, to Cloudflare's block response, which proves E1. Use
`/reset/request` here, not `/login`: custom rules run before rate limiting, so E2 answers a script
on `/login` and E1 never counts it.

Then, in a browser:

1. `/register` shows the widget, and a sign-up with it completes.
2. `/login` shows the widget, and a login with it completes.
3. In the developer tools, delete the `cf-turnstile` element from the form and submit. The form
   returns "Please complete the verification and try again."
4. As the admin, `/admin/resilience` lists `challenge.verify`, `auth.login.client` and
   `auth.register.client`.

In the metrics, `challengePassed` has valid rows, and `ChallengeConfiguration.challengeVerifier.turnstile`
has no violation.

## What to watch

All of these are on `naturalist.invariant.check`.

| Decision | A violation means |
|---|---|
| `turnstile` | The app is on the log-only verifier: the challenge is off |
| `hostnameChecked` | `expected-hostname` is blank: tokens from other sites would pass the hostname check |
| `credentialAccepted` | The vendor refused the credential: every challenge fails until it is fixed |
| `serviceAvailable` | Verification timed out or the breaker opened: users are being refused |
| `challengePassed` | A token failed. Some is normal; a surge is bots reaching the origin |
| `withinClientRate` | One address went over its limit |
| `withinRate` | A global backstop was hit: something beat the edge and the challenge, so investigate |

## When verification is down

Sign-ups return 503, and logins redirect to `/login?unavailable`, **including the admin's**.
Sessions already open keep working and the catalog stays readable. The app fails closed on purpose
(D6). The breaker retries about every 30 seconds, so a short outage clears by itself.

For a long outage, first check whether `serviceAvailable` or `credentialAccepted` is failing. For
a rejected credential, rotate it. For a vendor outage where sign-in must keep working:

1. Remove `challenge-credential` from the environment. **The flag alone does nothing:** while the
   credential resolves, the app stays on the live verifier.
2. Unset `naturalist.challenge.site-key`, so no widget renders.
3. Set `naturalist.challenge.log-only=true` and restart.

The challenge is now off, and a standing `turnstile` violation says so. Only the edge rules and
the rate limits protect login, so restore the settings and restart as soon as the vendor recovers.

## Local check with test keys

Run on 2026-11-03. Cloudflare publishes test site keys and credentials that work on any hostname,
in its Turnstile testing documentation. They exercise the widget and the failure paths only: **a
login or sign-up cannot succeed with them.** The always-passes credential answers `success: true`
with no `action` and the hostname `example.com`, so the app refuses it as an action mismatch. That
refusal is correct, so do not relax the check. Proving a success needs a real widget, with
`localhost` added to its hostnames.

1. Write the always-passes test credential to `temp/secrets/challenge-credential`. `temp/` is
   ignored by git.
2. Start the app against a copy of the database, because the standing test database is a fixed
   fixture and a sign-up would change it. Add
   `--naturalist.admin.username`, `--naturalist.admin.password`, `--naturalist.vision.provider=none`
   and `--naturalist.email.log-only=true` to these arguments, and leave `expected-hostname` unset:

   ```text
   --spring.datasource.url=jdbc:postgresql://localhost:5432/<the copy>
   --naturalist.secrets.source=file --naturalist.secrets.directory=<absolute path>/temp/secrets
   --naturalist.challenge.site-key=<a test site key>
   ```

3. Expect these results:

   | Site key | Widget | Submit |
   |---|---|---|
   | always passes | issues a token | "Please complete the verification and try again." on `/register` and `/login` |
   | always fails | issues no token | the same message on `/login` |

The message is the same in both rows, and the reason is recorded only on
`naturalist.invariant.check` (`actionMatches`, `challengePassed`), not in the log.

A local run without a credential needs only `--naturalist.challenge.log-only=true`.
