# Photo location privacy: strip metadata, keep a coarse area

**Status:** approved 2026-11-08, ready to plan. Closes launch item 1.1 ([work plan](2026-11-05-public-launch-work-plan.md)),
[PL-17](../notes/parking-lot.md), and pulls the location half of item 5.3 forward.

## Intent

A photo uploaded to the site must never disclose where the naturalist lives. Today, with JavaScript
off or on a direct POST, the original file is stored byte-for-byte, EXIF and GPS included, and served
to anyone at `/insects/images/{name}` with a one-year public cache. This is the site's
public-hosting blocker.

At the same time, *where* a photo was taken is genuinely useful to identification: a species whose
range does not reach the Pacific Northwest can be ruled out before the model looks at wing venation.
And it is interesting to readers. So instead of discarding location, the design keeps exactly one
coarse, deliberately disclosable form of it, the same everywhere: in the database, in the prompt sent
to the vision service, and on the public page. Nothing finer exists anywhere after the request ends.

The site is called the *backyard* naturalist; most photos are taken at home. The coarse form must be
coarse enough that the home is not recoverable from any number of photos.

## Goals

- No image stored or served by the app carries EXIF, XMP, IPTC, or text-chunk metadata, however it
  was uploaded.
- An observation may carry an **area**: the cell of a fixed geographic grid that contains the photo's
  location. Area is optional; many photos have no GPS and some users will clear it.
- Identification receives the area as range context.
- The area is shown on the public pages that show the observation, so the disclosure is visible and
  the same as what is stored.
- With JavaScript on, the exact coordinates never leave the device; the browser sends only the cell.
- Before submitting, the user sees the derived area and can clear it.

## Non-goals

- Naming cells after USGS quadrangles ("Portland quadrangle"). The grid is chosen so that this is a
  later lookup-table addition, not a redesign.
- Letting the user move the area or pick one by hand. Clear only.
- Server-side resizing or transcoding. The image-store design bans it and this design keeps that ban:
  the metadata rewrite removes whole segments/chunks and leaves the compressed image data untouched.
- Spatial queries ("observations within this polygon"). No PostGIS, no geospatial layer.
- Changing what the free-text `location` field is: it stays the user's own words.

## Anti-goals (what would make this a failure even if it works)

- Any representation whose centre or average over many photos is the true point. A circle centred on
  the GPS fix, or random jitter per photo, both fail this; a fixed grid does not.
- Precise coordinates reaching a third party. The vision vendor gets the cell, not the fix.
- Precise coordinates persisted anywhere, including logs, observations, or the `location` text field
  filled automatically. (A user may still *type* coordinates; that is their choice.)
- A stripping step that only some upload paths pass through.

## The grid

The cell is a 2.5-arc-minute cell of the latitude/longitude lattice: 1/24 of a degree on each side.
This is the USGS 7.5-minute quadrangle grid subdivided 3×3, so every cell nests exactly inside one
quadrangle and can be named after it later. At Oregon's latitude a cell is about 2.9 miles north–south
by 2.0 miles east–west. Pat judged a ~2-mile scale right for the ruling-out job and the privacy
requirement; a backyard is never recoverable because every photo from it lands in the same cell, and
the cell is the disclosure.

```
latIndex = floor(latitude  × 24)      range −2160 .. 2159
lonIndex = floor(longitude × 24)      range −4320 .. 4319
south = latIndex / 24,  north = (latIndex + 1) / 24
west  = lonIndex / 24,  east  = (lonIndex + 1) / 24
quadrangle = (floorDiv(latIndex, 3), floorDiv(lonIndex, 3))
```

Example: Portland, 45.52°N 122.68°W → cell (1092, −2945), bounds 45.500–45.542°N, 122.708–122.667°W.

The same two lines of arithmetic run in the browser (to show the area and to post the cell) and on
the server (to snap an EXIF fix on the JavaScript-off path). The server never trusts the browser's
arithmetic to be *honest*, only validates the indices are in range; a dishonest cell is the user's own
disclosure about themselves, not a safety problem.

## Design

### Kernel: `kernels/observation`

**`GridCell`** — `record GridCell(int latIndex, int lonIndex) implements ValueObject`.
`of(double latitude, double longitude)`, `bounds()` (south, north, west, east as degrees),
`quadrangle()` (the parent 7.5′ indices, for the later naming feature), and a `describe()` giving the
human form used on pages and in the prompt: `45.50–45.54°N, 122.71–122.67°W`. Invariants: indices in
range. A two-int record serialises naturally through Jackson for the test-entity sources; no custom
serializer.

**`Coordinates`** — `record Coordinates(double latitude, double longitude) implements ValueObject`,
request-scoped only. It exists so the EXIF reader has a typed result to hand to `GridCell.of`. It must
never be a component of any `Entity`, `Aggregate` or `ReadModel`; an ArchUnit rule in
`kernels/framework-test` (or the existing architecture test module) enforces that.

**`ImageUpload`** gains two methods, both pure functions over the bytes:

- `Optional<Coordinates> gps()` — reads EXIF GPS from a JPEG (APP1 Exif: TIFF header, byte order,
  IFD0 → GPS IFD pointer `0x8825`, tags 1–4, rational degrees/minutes/seconds, N/S and E/W refs).
  PNG `eXIf` and WebP `EXIF` chunks hold the same TIFF structure and share the parser. Anything
  malformed or absent → empty, observed, never thrown: a photo with broken EXIF is still a photo.
- `ImageUpload withoutMetadata()` — returns a new upload with metadata removed and image data
  untouched:
  - JPEG: drop `APP1` (Exif and XMP), `APP13` (IPTC/Photoshop) and `COM` segments. Keep `APP0`
    (JFIF), `APP2` (ICC profile; colour, not identity) and `APP14` (Adobe; decoders need it).
  - PNG: drop `eXIf`, `tEXt`, `zTXt`, `iTXt` chunks. Keep everything else; CRCs of kept chunks are
    unchanged.
  - WebP: drop the `EXIF` and `XMP ` chunks, clear the corresponding `VP8X` flag bits, and fix the
    RIFF size field.
  - Idempotent: a stripped upload stripped again is byte-identical. The browser's canvas output has
    no metadata, so the common path is a no-op.

**`ImageStore`** contract gains one sentence: *an implementation stores the upload with its metadata
removed (`ImageUpload.withoutMetadata()`); what `read` returns never carries metadata.*
`FilesystemImageStore.store` applies it unconditionally. This is the guarantee at the storage boundary:
it covers insects, plants, and any future caller, with or without JavaScript, whatever the controller
does. The `ImageStore` contract tests assert it with a GPS-tagged fixture.

### Client: `image-upload.js`

Finish the stubbed EXIF reader so it produces the GPS fix (same tag walk as the server). On file
change: if a fix is found, compute the cell, show it — *"Area: 45.50–45.54°N, 122.71–122.67°W"* with a
**Clear** button — and set two hidden fields `areaLat`, `areaLon`. Clear empties the fields and hides
the line. Submit posts the resized blob and the hidden fields; the coordinates themselves are never
put in any field. The old behaviour of writing coordinates into the `location` text field is removed.
The reader runs on the original file (before the canvas resize drops the EXIF). Both insects forms
(`identify.jte`, the add-photo form on `detail.jte`) already use this script via `id="identify-form"`.

### Server: controllers and commands

`InsectsController.identify` and the add-photo handler accept optional `areaLat`/`areaLon`. The area
is resolved once, in order: a posted cell (validated), else `upload.gps().map(GridCell::of)` for the
JavaScript-off path, else none. The raw fix is a local variable and nothing more.

**The strip happens in the controller, immediately after `gps()`**, and only the stripped
`ImageUpload` is handed to the command (my call, after review). The identification command builds the
vision `Image` from the upload it receives, so stripping only at the store would still send the raw
bytes, GPS included, to the vendor on the JavaScript-off path; `ImageDownscaler` passes a
small-enough JPEG through untouched. The storage-boundary strip stays as the second layer, idempotent
on already-stripped bytes. The identification command test asserts the vision request's bytes carry
no EXIF.

`InsectCommand.IdentificationCommand.identify(...)` and `PhotoCommand.add(...)` take
`@Nullable GridCell area`.

**Plants** has no photo upload path at all today (no `PlantCommand`, no `ImageUpload` use; the
console only reads seed photos), so plants gets only the persistence ripple: the record component,
`PlantObservationDbo`, the `plants.sql` columns, seeds `null`. Nothing on screen. A plants upload
path is its own feature (my call, after review).

**Identification context.** `kernels/vision` depends on `framework` only and must not learn about
`GridCell`, so `ImageMetadata` is unchanged. `InsectIdentificationCommandImpl` composes the existing
location context string from the user's typed location (if any) plus a sentence for the area:
*"Photographed within 45.50–45.54°N, 122.71–122.67°W. Treat this as the observation's location: rule
out species whose known range does not include it."* The system prompt's rule 2 already asks the
model to use location for range.

### Persistence

`OrganismObservation` gains `@Nullable GridCell area` after `location`. This ripples through
roughly 37 `new OrganismObservation<>(…)` sites in 18 files (kernel, insects api/core/rdbms/
repository-test including tests, plants api/rdbms/repository-test), the record's two `with*` methods,
the two `*ObservationDbo` classes, and both schemas (`area_lat INTEGER`, `area_lon INTEGER`, nullable,
in `insect_observation` and `plant_observation`; `catalog.sql` has no observation tables). Seed
observations carry `null`.

The identification command today copies `image.metadata().location()` into `observation.location`.
The area sentence composed for the vision request must stay out of that field: the observation's
`location` remains the user's typed words only, and the area lives in `area`.

**Already-stored raw uploads** (a developer's `data/images/<space>` directory holds whatever was
uploaded before this change, EXIF included) are out of scope: there is no production image root yet,
and it starts empty. Delete or re-upload local ones. There is no production database yet, so this is a schema change `db-init` absorbs for free;
after launch the same column would need the migration tool (item 5.1), which is why this moves now.
The standing test database is a fixed fixture: rerun `apps/test-db-seeder` after the schema change.

### Display

`observationGallery.jte` (insects) and the plants equivalent show the area beside the typed location
when present: *"Observed in the area 45.50–45.54°N, 122.71–122.67°W"*. When the quadrangle naming
feature arrives this becomes *"in the Portland quadrangle"* with the bounds as a title attribute.

## Tests

- `GridCell`: Portland; a southern/western point (negative indices); exact cell boundaries; the
  poles and the antimeridian; `quadrangle()` nesting (three consecutive cells share one quadrangle,
  including across zero).
- `ImageUpload.gps()`: a JPEG with big-endian and little-endian Exif; N/W and S/E refs; a JPEG with
  no GPS IFD; truncated and garbage APP1 → empty, violation observed.
- `ImageUpload.withoutMetadata()`: each format with metadata → no metadata and identical image data;
  idempotence; kept segments/chunks intact (JFIF, ICC, Adobe); WebP RIFF size and VP8X flags correct.
  Fixtures are built in the test from a tiny valid image plus hand-assembled segments, so no binary
  files are committed.
- `ImageStore` contract test: store a GPS-tagged JPEG, read it back, assert no EXIF.
- Insects console slice test: JavaScript-off POST of a GPS-tagged JPEG → the served bytes have no
  APP1 and the observation carries the expected cell; POST with `areaLat`/`areaLon` → that cell wins;
  POST with neither → `area` null; out-of-range indices → refused as a bad request.
- ArchUnit: no `Entity`/`Aggregate`/`ReadModel` record has a `Coordinates` component.
- The existing identification command test asserts the area sentence reaches the vision request's
  metadata when an area is given and is absent when not.

## Constraints

- Every third-party version in the root pom; a new dependency is a decision, not a convenience. The
  design assumes none: the EXIF walk and the segment rewrite are both small, and the browser needs the
  same reader in plain JavaScript anyway.
- `Observer` for argument checks; a producer handing a value out observes and returns rather than
  throws (`gps()` returning empty on bad EXIF).
- Repository mock methods validate their arguments; FK parity between mock and rdbms.
- Never weaken a gate to make this pass.

## Left to the builder

- Exact placement and markup of the area line and Clear button on the two forms (Pico defaults leak
  into custom markup; declare display explicitly).
- Whether `Coordinates` lives in `kernels/observation` beside `GridCell` or in `ImageUpload`'s package
  (both are `com.naturalist.observation` today).
- How the controller reports an out-of-range posted cell (redirect with an error code like the
  existing `?error=image`, or 400).
- Whether to test on a real phone before merging. iOS Safari converts HEIC to JPEG on upload and
  the canvas path handles it; whether EXIF survives that conversion decides if iPhone users get an
  area at all on the JavaScript-on path. Launch item 5.3 asked for a phone test; it is still
  worthwhile here.

## Decisions from the builder review (Pat, 2026-11-08)

1. **EXIF Orientation is kept.** Dropping the whole `APP1` would also drop tag `0x0112`, so a
   portrait phone photo uploaded with JavaScript off would display sideways (browsers rotate from
   that tag; the canvas path is unaffected because it draws the already-oriented bitmap). So
   `withoutMetadata()` reads the Orientation value before removing the `APP1` and, when it is present
   and not the default `1`, writes back a minimal `APP1`: the `Exif\0\0` header and a TIFF holding one
   IFD0 with the single Orientation entry (a fixed-shape ~40-byte structure, big-endian). No other
   tag survives. Tests cover each of the eight orientation values, absence, and the default.
2. **The 18 committed seed JPEGs are rewritten once** with the same `withoutMetadata()` (a one-off
   run, e.g. a throwaway test or `jshell` script, not a build step), so they keep their Orientation
   and lose everything else. The `ImageStore.read` guarantee then holds for seed photos and uploads
   alike, with no strip on `read`. A test over the classpath seed directories asserts every file is
   already stripped (`withoutMetadata()` is the identity on it), so a future seed photo cannot
   reintroduce metadata.

## Follow-ups this enables

- USGS quadrangle names for cells (lookup table, ~55k rows, or a per-state subset).
- Using the area to pre-filter candidate species from the catalog before calling vision.
- A small map tile or quad outline on the rank page.
