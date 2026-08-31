# Naturalists domain

A `Naturalist` (`NamedEntity<NaturalistName>`) is a person at Oak Vista — an
ecological actor (role, ecological stage), **not** an account. Authentication
lives in the separate `accounts` domain: `Account` (keyed by the opaque
`AccountName`, login by `email`) is linked to a `Naturalist` via the
`Naturalist.account` field, so the ecological record never carries a password.

- Read ports: `NaturalistQuery` (`byAccount(AccountName)` is the reverse
  lookup used by login) and `AccountQuery` (`getByEmail(String)`) in the
  `accounts` domain.
- Login username = the account `email`. `NaturalistUserDetailsService`
  (`management-console`) resolves the `Account` by email, then the linked
  `Naturalist` by `AccountName`, and builds the `NaturalistPrincipal`. Session
  identity is exposed to the console through the `CurrentNaturalist` seam
  (`com.naturalist.console.auth`); domain code does not depend on Spring
  Security.

See `docs/plans/2026-08-30-accounts-auth-redesign-design.md`.
