# Naturalists domain

A `Naturalist` (`NamedEntity<NaturalistName>`) is a person at Oak Vista — an
ecological actor (role, ecological stage), **not** an account. Authentication
credentials live in a **separate** record, `NaturalistCredential`
(`NamedEntity<NaturalistName>`, keyed 1:1 by the same slug), so the ecological
record never carries a password.

- `NaturalistCredential.passwordHash` is an encoded (`{bcrypt}$2a$…`) hash from the
  console's `PasswordEncoder`, never plaintext. Invariant: `entityName(name)` +
  `notBlank(passwordHash)`.
- Read ports: `NaturalistQuery`, `NaturalistCredentialQuery` (both `EntityQuery`).
- Login username = the `NaturalistName` slug. Session identity is exposed to the
  console through the `CurrentNaturalist` seam in `management-console`
  (`com.naturalist.console.auth`); domain code does not depend on Spring Security.

See `docs/plans/2026-07-06-naturalist-auth-design.md`.
