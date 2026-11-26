# Plant findByName: the nearest plant the catalog holds

Status: approved by Pat 2026-11-26; built on claude/named-values-step3-cbc835. Small change, one PR. A second PR adds a
look-alike search strategy after this one merges.

## Why

A visitor who asks for a plant the catalog does not hold today gets "Not in the catalog" and has to
guess which names exist. They should land on the closest thing the catalog does hold.

## Goals

- `PlantQuery.findByName(PlantRankName)`: answers the plant asked for, or failing that the nearest one
  the catalog holds, and says which.
- First strategy, the binomial: a species slug is the lowercased binomial (all 17 catalog species follow
  `genus-epithet`), so a missing species falls back to the genus named by its first segment.
- The species page uses it: a miss that finds its genus takes the visitor to the genus page, which says
  the species has not been added yet.

## Non-goals

- Look-alike names (typos, partial names): the second PR, as a second strategy behind `findByName`.
- Genus, family and order misses: their slugs name no parent, so the binomial strategy answers them
  exactly or not at all. They stay 404s until the search PR.
- Cultivar, lineage, program and constituent pages: unchanged.

## Anti-goals

- `getByName` must not degrade. It stays deterministic, so the rank pages' `has<Rank>` guards keep
  meaning "the query broke its contract".
- Never pass off the genus as the species: the visitor always sees that what they asked for is missing.

## Approaches

1. **A separate non-deterministic query (picked by Pat).** `findByName` next to an exact
   `getByName`. The degrade is reusable outside the console, and the search strategy slots in behind
   the same method.
2. Make `getByName` degrade. Rejected: it is the refactor the `has<Rank>` guards exist to catch.
3. Controller only: parse the slug in `PlantsController`. Rejected: not a query, other consumers can't
   reuse it, and the search strategy would land in the console too.

## Design

- **plants-api.**
  - `PlantQuery.findByName(PlantRankName name)` returns `Optional<PlantMatch>`.
  - `PlantMatch` is a record: `asked` (the `PlantRankName` requested), `found` (the `PlantRankName`
    answered), `plant` (the composed `Plant`) and `exact()` (`asked.equals(found)`).
  - Javadoc states that `getByName` is exact and `findByName` may answer a different rank.
  - *My call:* a match record rather than a bare `Optional<Plant>`, so a caller never has to infer from
    the `has*` predicates whether it got what it asked for. It also leaves room for the search PR's
    look-alikes.
- **plants-core.**
  - `PlantQueryImpl.findByName` tries `getByName(name)` first.
  - On a species miss, it takes the slug's first segment as a `PlantGenusName` and tries
    `getByName` on that.
  - A non-species miss, or a missing genus, is empty.
  - The fallback is counted at INFO under `findByName` (a record not added yet, not a fault).
- **plants-console.**
  - `detail`: an exact match renders the species page as today.
  - A genus match redirects to `/plants/genera/{genus}?missing={species}`.
  - Empty is the INFO 404 as today.
  - `genusDetail` takes an optional `missing` request parameter, typed as `PlantSpeciesName` (a value
    that is not a slug is dropped, not shown). When present, the genus page shows "No record for
    *{missing}* yet; here is its genus."
  - *My call:* a redirect rather than rendering the genus at the species URL, so the address bar and
    bookmarks match the page shown.
- **Tests.**
  - The `PlantQuery` contract test covers exact, genus fallback, missing genus (empty) and a
    non-species miss (empty).
  - The plants edge `@WebMvcTest` covers the redirect, the note on the genus page, a bad `missing`
    value dropped, and the fallback counted at INFO.

## Left to the builder

- Wording and placement of the "not added yet" note on the genus page.
- Whether the binomial split is a static helper on `PlantSpeciesName` or private to the impl
  (prefer the impl unless a second caller appears). The split landed as public
  `PlantMatch.genusOf`, because `PlantMatch.invariants()` is its second caller.
