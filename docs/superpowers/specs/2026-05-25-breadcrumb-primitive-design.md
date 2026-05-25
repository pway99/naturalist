# Taxonomic-Scope Breadcrumb Primitive — Design Spec

A reusable breadcrumb trail for the insects console that renders the full
taxonomic lineage from clade root through Linnaean ranks, unifying two
existing rendering patterns (clade lineage on listing pages, ad-hoc
taxonomy links on detail pages) into a single component.

**Scope:** insects domain only. Other domains build their own equivalent
when needed; extraction happens when two consumers prove the need.

**Phase 0 deliverable.** Reused by Phase 2 for identification session-scope
rendering and pending-organism display.

---

## Decisions

| Question | Decision | Rationale |
|----------|----------|-----------|
| Domain scope | Insects only | Per-domain per the identification roadmap; kernel extraction deferred |
| Clade prefix | Shown by default; caller controls by omitting clade segments from the list | Listing pages show full trail; detail pages could omit if needed — no template-level flag required |
| Clickable segments | Clade segments non-clickable; order and below clickable | Rule: "has a catalog page or not." Orders now have `/insects/orders/{name}` |
| Current-page segment | Present, non-linked, bold | Standard breadcrumb convention; completes the trail visually |
| What it replaces | Both clade lineage in `cladeIntro.jte` AND ad-hoc taxonomy links on detail pages | Single rendering path for the lineage trail everywhere |
| Data shape | Pre-built segment list `(label, url-or-null, isCurrentPage)` | Template is a pure renderer; controller assembles from entities already in hand; Phase 2 reuses without pretending to have insect entities |
| Assembly pattern | Controller helper methods | Builder class adds ceremony for 5-10 lines of list construction |

---

## Data Type

A record in `com.naturalist.insects.console`:

```java
public record BreadcrumbSegment(String label, String url, boolean currentPage) {
    public static BreadcrumbSegment link(String label, String url) {
        return new BreadcrumbSegment(label, url, false);
    }
    public static BreadcrumbSegment text(String label) {
        return new BreadcrumbSegment(label, null, false);
    }
    public static BreadcrumbSegment current(String label) {
        return new BreadcrumbSegment(label, null, true);
    }
}
```

Three factory methods cover the three segment flavours:

- **`link`** — clickable rank segment (order, family, genus, species when not the current page)
- **`text`** — non-clickable label (clade segments: Animalia, Arthropoda, Insecta)
- **`current`** — current-page marker (final segment, rendered bold, no link)

---

## JTE Template

New file `insects/breadcrumb.jte`. Accepts `List<BreadcrumbSegment>`, renders the trail:

```html
@import com.naturalist.insects.console.BreadcrumbSegment
@import java.util.List

@param List<BreadcrumbSegment> segments

<nav class="breadcrumb-trail" aria-label="Taxonomic placement">
    @for(int i = 0; i < segments.size(); i++)
        @if(i > 0)
            <span aria-hidden="true"> › </span>
        @endif
        !{var s = segments.get(i);}
        @if(s.currentPage())
            <span class="breadcrumb-current"><strong>${s.label()}</strong></span>
        @elseif(s.url() != null)
            <a href="${s.url()}">${s.label()}</a>
        @else
            <span>${s.label()}</span>
        @endif
    @endfor
</nav>
```

- Reuses the existing `›` separator convention from `cladeIntro.jte`
- `aria-label="Taxonomic placement"` carries over from the current clade lineage
- Uses `<nav>` since this is navigational content

---

## Assembly Logic

Private helper methods on `InsectsController`. The controller already fetches
the full entity chain at every detail route.

One method builds the clade prefix (shared across all pages):

```java
private List<BreadcrumbSegment> cladePrefix() {
    return CladeTraversal.ancestry(new Insecta()).reversed().stream()
            .filter(c -> !(c instanceof Eukaryota))
            .map(c -> BreadcrumbSegment.text(c.displayName()))
            .toList();
}
```

Per-rank methods compose the prefix with rank segments:

```java
// Order detail: Animalia › Arthropoda › Insecta › **Coleoptera**
private List<BreadcrumbSegment> breadcrumbToOrder(InsectOrder order) {
    var segments = new ArrayList<>(cladePrefix());
    segments.add(BreadcrumbSegment.current(order.order().value()));
    return segments;
}

// Family detail: Animalia › Arthropoda › Insecta › Coleoptera › **Carabidae**
private List<BreadcrumbSegment> breadcrumbToFamily(InsectFamily family, InsectOrder order) {
    var segments = new ArrayList<>(cladePrefix());
    segments.add(BreadcrumbSegment.link(order.order().value(),
            "/insects/orders/" + order.name().value()));
    segments.add(BreadcrumbSegment.current(family.family().value()));
    return segments;
}

// Genus detail: ... › Coleoptera › Carabidae › **Carabus**
private List<BreadcrumbSegment> breadcrumbToGenus(InsectGenus genus,
                                                  InsectFamily family,
                                                  InsectOrder order) {
    var segments = new ArrayList<>(cladePrefix());
    segments.add(BreadcrumbSegment.link(order.order().value(),
            "/insects/orders/" + order.name().value()));
    segments.add(BreadcrumbSegment.link(family.family().value(),
            "/insects/families/" + family.name().value()));
    segments.add(BreadcrumbSegment.current(genus.genus().value()));
    return segments;
}

// Species detail: ... › Coleoptera › Carabidae › Carabus › **Carabus nemoralis**
private List<BreadcrumbSegment> breadcrumbToSpecies(InsectSpecies species,
                                                    InsectGenus genus,
                                                    InsectFamily family,
                                                    InsectOrder order) {
    var segments = new ArrayList<>(cladePrefix());
    segments.add(BreadcrumbSegment.link(order.order().value(),
            "/insects/orders/" + order.name().value()));
    segments.add(BreadcrumbSegment.link(family.family().value(),
            "/insects/families/" + family.name().value()));
    segments.add(BreadcrumbSegment.link(genus.genus().value(),
            "/insects/genera/" + genus.name().value()));
    segments.add(BreadcrumbSegment.current(
            genus.genus().value() + " " + species.epithet().value()));
    return segments;
}
```

---

## Template Integration

### Detail pages

Replace ad-hoc taxonomy rendering with the breadcrumb call. Each detail
template adds `@template.insects.breadcrumb(segments = breadcrumb)` between
the nav and the `<h1>`:

- **`order.jte`** — add breadcrumb (currently has no taxonomy links)
- **`family.jte`** — replace `<p class="taxonomy"><a href="...">order</a></p>`
- **`genus.jte`** — replace `<p class="taxonomy">` block (two links with `·`)
- **`detail.jte`** (species) — replace `<p class="taxonomy">order · family · genus species</p>`

### Listing pages

`cladeIntro.jte` delegates its lineage trail to the breadcrumb:

- Remove the inline `CladeTraversal.ancestry()` loop (lines 10-20)
- Replace with `@template.insects.breadcrumb(segments = breadcrumb)`
- Add `@param List<BreadcrumbSegment> breadcrumb`
- Drop `CladeTraversal`, `Eukaryota`, `Insecta` imports
- The `<details>` description block (lines 21-39) stays unchanged

Controller changes: each handler that calls `addCladeDescription(model)` also
adds the clade-only breadcrumb via `model.addAttribute("breadcrumb", cladePrefix())`.

---

## CSS

Rename `.clade-lineage` to `.breadcrumb-trail` in `naturalist.css`:

```css
.breadcrumb-trail {
    font-size: 0.85rem;
    color: var(--pico-muted-color);
    margin: 0 0 0.25rem;
    text-shadow: var(--text-shadow-subtle);
}

.breadcrumb-trail a {
    color: var(--pico-muted-color);
    text-decoration: none;
}

.breadcrumb-trail a:hover {
    text-decoration: underline;
}
```

Links inherit the muted color so the trail stays visually quiet. Underline on
hover gives affordance without clutter at rest. The current-page `<strong>`
uses the same muted color but stands out via font weight. The old
`.clade-lineage` rule is deleted.

---

## File inventory

| File | Action | Lines (est.) |
|------|--------|-------------|
| `BreadcrumbSegment.java` | new | ~15 |
| `insects/breadcrumb.jte` | new | ~15 |
| `InsectsController.java` | modify | +30 (helpers + model attrs) |
| `cladeIntro.jte` | modify | -10, +2 (delegate to breadcrumb) |
| `order.jte` | modify | +1 (add breadcrumb call) |
| `family.jte` | modify | +1, -1 (replace taxonomy link) |
| `genus.jte` | modify | +1, -3 (replace taxonomy links) |
| `detail.jte` | modify | +1, -3 (replace taxonomy text) |
| `naturalist.css` | modify | +8, -4 (rename + extend rule) |

No new queries, routes, entities, or modules.

---

## Phase 2 reuse path

The identification workflow builds its own `List<BreadcrumbSegment>` from
session scope data — it does not need insect entities. The segment record
and JTE template are the reusable parts; the assembly logic is
domain-specific. If plants later needs the same primitive, the record and
template extract to a shared location; each domain writes its own assembly.
