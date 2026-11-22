# Photo sizes design

Written 2026-11-11, before launch item 1.5 (account recovery). Two changes to how photos are kept
and served:

1. **No stored photo is larger than 1024 px on its longest edge.** The server enforces the limit
   the upload form already applies in the browser.
2. **Each photo gets one small tile copy, 640 px.** Every page that shows photos uses the tile.
   Clicking a tile opens the photo in a view over the page.

Photos stay on the filesystem behind the `ImageStore` port. Storing them in Postgres was
considered and rejected (see "Not in Postgres").

## Why

Cost: every byte of a photo that leaves the host is paid for. The host's bandwidth allowance pays
for it, and so does the naturalist's mobile data. Cloudflare's cache (ADR-027) spares the host
repeat requests, but it doesn't shrink what each visitor downloads.

**What exists today.**
- The upload form shrinks every photo in the browser before sending it: `image-upload.js`, via
  `resizeImage(file, 1024)`, re-encoded as JPEG at quality 0.85.
- A normal upload is therefore about 1024 px and a few hundred KB.
- With JavaScript off, or with a direct POST, the server stores whatever arrives, up to
  `ImageUpload.MAX_BYTES` (20 MB). That is the photo that took the app down in PL-16 before reads
  were streamed.
- The seed photos aren't held to the browser's limit either:
  - 11 are 1024 px (captured console uploads);
  - 16 are 2048 px;
  - 2 are 4032 px.
- A gallery of twelve 1024 px photos is about 4 MB. Twelve tiles are about 0.8 MB.

## Decided by Pat

- **Downscale on the server, not by trusting the client.** The store applies the 1024 px limit
  itself.
- **One tile size: 640 px.** Cards render at least 320 CSS px wide, and phones put 2–3 device
  pixels into each CSS pixel, so a 320 px tile would look soft. Pat wants the site to look sharp
  while it runs on a tight budget. The size is one setting, so QA can revisit it.
- **Every place that shows photos uses the tile.** Pat will decide by manual QA which places, if
  any, get more. Do not try to guess those places now.
- **Clicking a photo opens a larger view over the page.** This is a lightbox-style overlay, not a
  new page. It shows the stored photo, which is at most 1024 px.
- **Not in Postgres.** Photos stay on the filesystem.
- **A missing tile never breaks the page.** The card or gallery still renders, and the missing
  photo shows a "photo not found" placeholder instead of a broken-image icon.
  - The placeholder is a small static image in the app's `static/images/`, the same for both
    consoles.
  - It is swapped in client-side when the tile fails to load, through the `<img>`'s `onerror`.
    The CSP sets no `script-src`, so inline handlers are allowed, as the templates already use.
  - It must keep working on cycling cards, so `card-image-rotate.js` must not skip or break on a
    swapped image.
  - The tile endpoint's 404 is sent with `Cache-Control: no-store`, so the placeholder disappears
    once the tile exists.
  - The server observes each missing tile (Observer, `WARN`), so the defect is counted rather than
    hidden.
  - Without JavaScript, the browser's own broken-image rendering and the `alt` text remain. That is
    acceptable.

## Decided by Claude (check these)

- **Store-time processing.** `store` decodes the upload once, safely (see "Bounded decoding"
  below), and then does two things:
  - **The photo.**
    - **Longest edge 1024 px or less:** keep the stripped upload exactly as today. Its bytes are
      unchanged, so the JS-resized uploads that make up almost all real traffic pass through
      without being re-encoded.
    - **Longer than 1024 px:** keep instead a downscaled JPEG, 1024 px on its longest edge, at
      quality 0.85. It is flattened onto white if it has transparency, rotated upright using the
      Exif orientation, and carries no metadata. The oversized upload itself is not kept.
  - **The tile.** A JPEG 640 px on its longest edge, at quality 0.85, rotated upright, with no
    metadata. A photo smaller than 640 px is not upscaled; its tile is just the photo re-encoded.
- **Upright copies.** The stored photo keeps only its Exif orientation tag
  (`ImageMetadataSegments.orientationOnlyApp1`), and browsers apply that tag themselves. Any image
  the store re-encodes must instead apply the orientation to its pixels.
  `ImageDownscaler` (vision adapter) does not apply orientation, so don't reuse it as-is.
- **WebP is decoded.** The JDK's `ImageIO` cannot read WebP. Add TwelveMonkeys `imageio-webp`, a
  read-only, pure-Java `ImageIO` plugin. Its version goes in the root `pom.xml` `<properties>`
  (root `CLAUDE.md`). This reverses `ImageDownscaler`'s "no image library is added" stance for the
  store; whether vision's downscaler also uses it is the builder's call.
- **Bounded decoding.** Follow `ImageDownscaler`:
  1. Read the declared dimensions first.
  2. Refuse anything above a pixel ceiling as an invalid upload. That class uses 100 Mpx as
     `MAX_PIXELS`.
  3. Decode with source subsampling, so the raster held is never much larger than about twice
     1024 px.

  An undecodable upload is refused like any other invalid one, with nothing written.
- **Store concurrency and time.** Put a bulkhead on concurrent `store` calls, so several large
  uploads cannot stack decodes, and give `store` its own timeout, separate from the 2 s read
  timeout. The builder picks the numbers.
- **All or nothing.** If writing the photo or the tile fails, neither is left behind and the upload
  is refused. `delete` removes both.
- **A missing tile is a 404, never the photo.** Every stored photo gets its tile, through the store
  and the backfill. Falling back to the photo would quietly undo the cost saving.
- **Seed photos are rewritten once, in the repo.**
  - The 18 checked-in seed photos above 1024 px are downscaled at source, the way 1.1 stripped
    their metadata. The classpath then holds only photos within the limit, and the repo shrinks.
  - `SeedImagesCarryNoMetadataTest`, or a sibling test, also asserts the 1024 px limit.
  - Tiles are derived and never checked in.

## What changes

**Kernel port (`kernels/observation`).**
- A read names what it wants: the photo or its tile. For example, an enum `ImageVariant { PHOTO,
  TILE }`, or a sibling `readTile`.
- `StoredImage` keeps its streaming shape: media type, size, `Content`.
- `store` still returns one `FileName`. The tile is addressed by that name, so callers and the
  persisted `InsectImage.resourceName` and plant image records are unchanged.
- The `ImageStore` javadoc states the 1024 px guarantee.

**Filesystem store (`kernels/image-store-filesystem`).**
- `store` and `delete` change as above.
- The on-disk layout of tiles is the builder's call. It must keep the existing path-traversal
  guard, and must never let a client name reach a path.

**Consoles (`insects-console`, `plants-console`).**
- `GET /insects/images/{filename}` keeps serving the photo, as it does today.
- A tile URL is added, for example `GET /insects/images/tile/{filename}`.
- The same applies to `/plants/images/`.
- Both keep `Cache-Control: max-age=1y, public, immutable`, plus the streaming and bulkhead 503
  from 1.6.
- Production has never run, so no edge or browser has cached these URLs. Nothing needs purging.

**Templates.** All three photo sites use the tile as a plain `src`, with no `srcset`, so no screen
pulls the 1024 px photo for a thumbnail:
- `insects/cardImages.jte`: the card tiles on the list and rank pages.
  - They are links to the rank page already, so they keep that behaviour and don't open the view.
  - `card-image-rotate.js` cycles them.
- `insects/observationGallery.jte`: the rank-page photo gallery. Its photos open the view.
- `plants/gallery.jte`: the plant photo grid. Its photos open the view.

**The larger view.**
- A small script beside `card-image-rotate.js` in
  `apps/management-console/src/main/resources/static/js/`.
- It opens a `<dialog>` showing the clicked photo's 1024 px image.
- It closes on Escape, on a click outside the photo, and on a close button.
- Without JavaScript, a gallery photo is a plain link to the photo URL.
- The CSP (`connect-src 'self'; form-action 'self'; frame-ancestors 'none'`) restricts neither
  images nor scripts.

**Seeder and `db-init` (`apps/test-db-seeder`).**
- `Seeding.copySeedPhotos` copies seed photos into the store's root, as today.
- Tiles come from the backfill, below.

## Backfill

Every photo already in a store's root must meet the limit and have its tile:
- seed photos;
- uploads in a developer's local `data/images`;
- uploads in the local Docker `images` volume.

Production has no uploads yet.

The step:
- downscales any photo over 1024 px in place;
- writes any missing tile;
- runs on the same code path as `store`;
- is idempotent.

`db-init` currently skips everything when the database is already initialized ("Database already
initialized; nothing to do"). So the backfill must run whether or not the database step ran,
otherwise it never reaches the local Docker volume. The builder decides where it lives.

## Not in Postgres

This was considered on 2026-11-11 and rejected by Pat. The database would offer:
- a single backup;
- photo and row in one transaction;
- no image volume.

Against that:
- every image request would hold a pooled connection, competing with page queries;
- the driver reads a `bytea` whole into the heap, which loses 1.6's streaming;
- dumps would be heavier, with more WAL and vacuum work on a 4 GB VM;
- photos are opaque blobs that nothing queries.

Revisit only if keeping database and photo backups in step proves painful. The `ImageStore` port
makes it one new adapter plus a one-off copy.

## A refused photo costs no identification

Decided by Pat, 2026-11-11.

`InsectIdentificationCommandImpl.reserveStoreAndCatalogue` runs, in order:
1. the `vision.identification` bulkhead;
2. `imageStore.store(...)`;
3. `budget.reserve(naturalist)`;
4. the vision call.

(Before any of them the rate gate runs, and the controller and command call the header-only
`requireStorable`, so a header that does not decode or declares more than 100,000,000 pixels is
refused with `?error=image` before anything is stored.)

`store` can refuse a photo: its body does not decode. Because `store` runs before
`budget.reserve`, a refused photo costs no identification: the controller answers it with
`?error=image`. The refusal is counted once, at WARN (`FilesystemImageStore.store.decodable`;
Pat, 2026-11-22).

`store` failing for any other reason is infrastructure, not a bad photo, and it also costs no
identification, since nothing was reserved yet:
- a full `image.store.write` bulkhead is a `BUSY` refusal, recorded as an `identification-refused`
  usage event like a full vision bulkhead (Pat, 2026-11-22), and answered `?limit=BUSY`;
- a write timeout or an I/O failure is a 500, counted where it happened, with no charge;
- a decoded image the store cannot re-encode is a fault (`PhotoScaler.fitJpeg.encoded`, ERROR), a
  500 with no charge.

A budget refusal, or any failure after the store, deletes the stored upload.

**The cost of this order (Pat, 2026-11-22, accepted).** An over-budget call now decodes,
re-encodes and writes the upload before its budget refusal, then deletes it. That extra work is
bounded by the rate gate (one permit per call) and by the `vision.identification` bulkhead, which
both run before the store.

The photo-add command (`InsectPhotoCommandImpl`) has no budget, so it needs no reordering.

## Out of scope

- **Vision identification's own downscale.** `ImageDownscaler` sends 1568 px; it never reads from
  the store.
- **Serving anything larger than 1024 px.** No originals are kept.
- **Responsive `srcset`, other tile sizes, AVIF or WebP output.**
- **`image-upload.js`.** Its 1024 px resize stays; the server now backs it up.

## Anti-goals

The design has failed, even if it works, if:
- a stored photo exceeds 1024 px;
- a thumbnail position loads the photo instead of the tile;
- the server decodes or resizes while serving a request;
- a missing tile silently falls back to the photo, breaks a card's layout, or goes uncounted;
- a re-encoded image carries metadata or shows sideways;
- an oversized upload's original is kept anywhere.

## Left to the builder

- The tile layout and naming on disk.
- The pixel ceiling, store bulkhead and store timeout numbers.
- Where the backfill lives.
- The port shape for the variant.
- How the larger view looks.
- Whether vision's downscaler adopts WebP decoding.
- How the work splits into slices. Expect the kernel and store change first, then the seed
  rewrite and backfill, then the consoles, templates and view.

Tests follow the repo's rules: Observer-based argument checks, the N+1 gate, the rewrite gate, and
the credential grep.
