# Secret rotation

Decisions: [ADR-028](../adr/ADR-028-secrets-and-model-spend-protection.md). Design and
first draft: [design plan §6](../plans/2026-11-02-secrets-and-endpoint-protection-plan.md).

The app reads every deployment secret through the kernel `SecretProvider` under a
`SecretName` that does not say what it unlocks. The model credential is `model-credential`,
read from the `MODEL_CREDENTIAL` environment variable (`naturalist.secrets.source:
environment`) or from `<directory>/model-credential` (`naturalist.secrets.source: file`). A
secret is resolved once, at startup.

A value is never written down, logged or shown. Its **fingerprint** is: the first eight hex
digits of its SHA-256. Compute one locally:

```bash
printf %s "$SECRET" | shasum -a 256 | cut -c1-8
```

The fingerprint labels a credential in the log below; it is never shown by the app. The app
has no page that lists secrets. Which credential is live is proved by behaviour: an
identification still works after the old credential is deactivated.

## Routine rotation

Do this quarterly, whenever an operator changes, and after any rehearsal.

1. In the model provider's console, under the production workspace, create a new credential
   labelled with today's date. Copy it once.
2. Compute its fingerprint locally. Record the date, the credential's label and the
   fingerprint in the log below. Never record the value.
3. Update the deployment's secret store with the host's command. Do not deactivate the old
   credential yet.
4. Restart or redeploy.
5. Smoke test: one identification in the deployment with a known photo. It counts against
   the budget, which is fine.
6. In the provider's console, deactivate the old credential.
7. Smoke test again. A pass proves the new credential is the live one; a failure means the
   deployment is still on the old one, so reactivate it and go back to step 3. Delete the old
   credential after seven days.
8. Close the log entry with the time the old credential was deactivated.

## Emergency rotation

Use this when a leak is suspected.

1. Deactivate the old credential first, and accept a few minutes without identification.
2. Run routine steps 1 to 5, then step 8. Step 5's smoke test is already the proof, since the
   old credential is dead.
3. Afterwards, check the workspace usage in the provider's console for anomalous spend.
4. Check the per-naturalist counters at `/admin/usage` to find where it came from.

## Rehearsal

Before the first public deployment, run the whole routine procedure in the pre-public
environment:

- Time each step.
- Note in the log every observation that did not match this runbook.
- Fix whatever it exposes.

The first production rotation then retires the pre-publication credential.

## Rotation without a restart

Rotation without a restart is deliberately not built, because every candidate host restarts
on a secret change anyway. If that ever changes, the one-step addition is to resolve again
on an authentication error.

## Rotation log

| Date | Credential label | Fingerprint | Old credential deactivated at | Notes |
|------|------------------|-------------|-------------------------------|-------|
| _pending_ | `naturalist-dev` | _to be recorded by Pat_ | — | pre-publication development credential (design plan §7 step 0) |
