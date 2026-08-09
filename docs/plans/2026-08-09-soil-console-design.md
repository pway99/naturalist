# Soil Console (facts viewer) — Design

**Date:** 2026-08-09
**Status:** Design — approved in brainstorming; pending spec review.
**Program:** Soil measurement monitoring. This is the first consumer of the shipped Slice 1
model — a read-only viewer to validate the `SoilProfile` assembly against a real UI. The
**CropProfile / interpretation** slice follows and turns this plain viewer into the interpreted,
color-coded report.

## Goal

Surface the assembled `SoilProfile` in the management console so we can *see* the model work
end-to-end in the real Spring app: browse the Oak Vista soil profiles, open one, and view its
dated analyses with the grouped nutrient panel and physical characteristics — the real March 2026
FGL data, rendered as structured facts.

## Scope

**In scope**
- A new **`soil-console`** module (Spring `@Controller` + JTE templates + data `@Configuration`),
  composed by `apps/management-console`, mirroring `insects-console`.
- Create the deferred **`soil-test-context`** module (mirrors `insects-test-context`): wires the
  repository mocks → entity queries → `SoilProfileFactory` → exposes `soilProfileQuery()` and
  `soilProfileInfoQuery()`. This is the console's (and future integration tests') wiring seam.
- Two read-only routes and their templates (below).
- A "Soil" entry in the management-console nav.
- JTE template tests rendering both templates against the fixture data.

**Out of scope (where each lands)**
- **Status / optimum / color / interactive re-interpretation** — the **CropProfile slice** (turns
  this plain table into the interpreted report).
- **Monitoring / trajectory** (the gypsum-rehab time series) — a later slice, built on crop.
- **Editing** (console stays read-only; data comes from fixtures) and **images** (no soil-photo
  concept) — deferred independently, not planned.

## Design

### Module + data wiring

- **`soil-test-context`** (new module, mirrors `insects-test-context`): a `SoilTestContext` class
  in `com.naturalist.soil` (split-package with `soil-core` impls) with `SoilTestContext.create(db)`
  exposing `soilProfileQuery()` (assembled aggregate) and `soilProfileInfoQuery()` (list/page).
  New-module scaffolding per `domains/CLAUDE.md`: module dir + pom, `<module>` in
  `domains/soil/pom.xml`, root `dependencyManagement` entry (`${project.version}`, SOIL section).
- **`soil-console`** module: depends on `soil-api`, `soil-test-context`, `spring-web`,
  `spring-context`, `jakarta.servlet-api`. A `SoilDataConfiguration` `@Configuration` provides the
  `NaturalistDatabase`-backed `TestEntitySource` beans and the assembled `SoilProfileQuery` /
  `SoilProfileInfoQuery` (via `SoilTestContext`), following `InsectDataConfiguration`.

### Routes + templates

- **`GET /soil`** → `SoilProfileInfoQuery.findPage(PageRequest.console)` → the 4 profiles, each row:
  profile name, zone (`zoneName`/`subZoneName`), crop, latest sample date. Template `soil/list.jte`.
- **`GET /soil/{soilProfileName}`** → `SoilProfileQuery.getBySoilProfileName` → the assembled
  `SoilProfile`. Template `soil/profile.jte`:
  - **Header** — profile name, zone/sub-zone, crop.
  - **Per dated `LabAnalysis`** (most recent first): analysis header (sample date, lab, sample id),
    then the **nutrient panel** as three small tables — **Primary** (nitrate-N, P₂O₅, K-exch,
    K-sol), **Secondary** (Ca/Mg/Na exch+sol, sulfate), **Micro** (Zn, Mn, Fe, Cu, B, Cl) — each row
    *nutrient · value · unit*; and the **physical characteristics** (pH, EC, CEC, limestone,
    physical saturation, and the 5 cation base-saturation percentages).
  - Facts only — value + unit, no status/color.
  - Not-found `{soilProfileName}` → a simple "profile not found" render (or redirect to `/soil`).

### Nav

Add a "Soil" link to the management-console shell alongside the existing domain entries (the same
registration the other `-console` modules use).

### Testing

- JTE template tests (pattern: `InsectsListTemplateTest`): render `soil/list.jte` against the 4
  profiles and `soil/profile.jte` against the assembled box1 profile; assert real values appear —
  e.g. box1 soluble Ca `6.99 lbs/1000 ft²`, pH `7.2`, CEC `44.9`, and that all three nutrient
  groups render. This validates the assembly a second time through the actual render path.
- Follow the console template-test conventions (the shared `page.jte` classpath rule: the soil
  console module must not pull in Spring Security / servlet-app types into templates; read the
  insects-console template tests + the `page.jte` trap before writing).

## Open items for the implementation plan

- Exact management-console nav registration point + how domain consoles are component-scanned.
- Whether `soil/list.jte` groups the backyard sub-zones under "Backyard" or lists the 4 flat
  (lean: flat for now; grouping is cosmetic and can follow).
- `PageRequest.console` availability for the profile list (per the paging convention).
