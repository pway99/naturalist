# Insect Page Images

Add image carousels to every insect listing and detail page. Each card shows images
gathered from all descendants of the card's entity in the taxonomic hierarchy.

## Current state

Images display on two pages only:
- **Species list** (`list.jte`) — 3.5s rotating carousel per species card, backed by
  `Map<InsectSpeciesName, List<InsectImage>>` built in the controller.
- **Species detail** (`detail.jte`) — static photo gallery grid.

Order, family, and genus listing/detail pages show no images.

## Design

### ImageGallery

A new `BehavioralCollection<InsectImage>` in `InsectEntityCollections`. Flat list of all
images for the page (inherited from `BehavioralCollection`), plus a secondary
`Map<InsectRankName, ImageCollection>` index for per-card lookup.

```java
final class ImageGallery extends BehavioralCollection<InsectImage> {
    private final Map<InsectRankName, ImageCollection> byEntity;

    ImageGallery(Map<InsectRankName, ImageCollection> byEntity) {
        super(byEntity.values().stream()
                .flatMap(BehavioralCollection::stream)
                .toList());
        this.byEntity = Map.copyOf(byEntity);
    }

    public static ImageGallery of(Map<InsectRankName, ImageCollection> byEntity) {
        return new ImageGallery(byEntity);
    }

    public static ImageGallery empty() {
        return new ImageGallery(Map.of());
    }

    public ImageCollection forEntity(InsectRankName name) {
        return byEntity.getOrDefault(name, ImageCollection.empty());
    }
}
```

Templates receive `ImageGallery gallery` and call `gallery.forEntity(entity.name())` to
get the carousel images for each card.

### JTE component: `insects/cardImages.jte`

Shared carousel fragment. Parameters:

| Param | Type | Purpose |
|-------|------|---------|
| `images` | `List<InsectImage>` | images to rotate through |
| `linkUrl` | `String` | card click-through URL |
| `alt` | `String` | image alt text |

Renders the `<figure class="card-image" data-rotate="...">` block with `<img>` tags.
Caller wraps in `@if(!images.isEmpty())`. The rotation JavaScript stays at the bottom
of each page template (same pattern as today's `list.jte`).

### Controller changes

Each listing/detail method builds an `ImageGallery` by walking the hierarchy:

| Method | Card entity | Descendants gathered |
|--------|-------------|---------------------|
| `orders()` | `InsectOrder` | families (via `forOrderName`) -> genera (via `forFamilyName`) -> species (via `forGenusName`) -> images (via `forParentName`) per species/genus/family |
| `orderDetail()` | `InsectFamily` | genera (via `forFamilyName`) -> species (via `forGenusName`) -> images per species/genus/family |
| `families()` | `InsectFamily` | genera (via `forFamilyName`) -> species (via `forGenusName`) -> images per species/genus/family |
| `familyDetail()` | `InsectGenus` | species (via `forGenusName`) -> images per species/genus |
| `genera()` | `InsectGenus` | species (via `forGenusName`) -> images per species/genus |
| `genusDetail()` | `InsectSpecies` | images (via `forParentName`) directly |

A private helper method in the controller collects images for a given rank entity —
including images tagged directly to the entity itself plus all descendants — and returns
`ImageCollection`:

- `imagesForOrder(InsectOrderName)` -> self + families -> genera -> species
- `imagesForFamily(InsectFamilyName)` -> self + genera -> species
- `imagesForGenus(InsectGenusName)` -> self + species

Each listing method loops over the page content, calls the appropriate helper, and
accumulates into a `Map<InsectRankName, ImageCollection>` which gets wrapped as
`ImageGallery.of(map)`.

### Template changes

Six templates gain the `ImageGallery gallery` parameter and render carousels on each card
using the `cardImages.jte` component:

1. `orders.jte` — order cards
2. `order.jte` — family cards within an order
3. `families.jte` — family cards
4. `family.jte` — genus cards within a family
5. `genera.jte` — genus cards
6. `genus.jte` — species cards within a genus

### Species list refactor

`list.jte` currently takes `Map<InsectSpeciesName, List<InsectImage>>`. Refactor to take
`ImageGallery` instead, using the same `cardImages.jte` component. The controller's
existing image-gathering loop becomes a call to the genus-level helper per species.

### Carousel behavior

Same as today's species list: 3.5-second rotation interval, respects
`prefers-reduced-motion: reduce`, toggles `card-image-active` CSS class.
