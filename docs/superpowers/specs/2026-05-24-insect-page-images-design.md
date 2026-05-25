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

### BehavioralMap (kernel)

New abstract base in `kernels/framework` alongside `BehavioralCollection`. Extends
`BehavioralCollection<V>` and adds a keyed index built from a grouping function.

```java
public abstract class BehavioralMap<K, V extends Observable>
        extends BehavioralCollection<V> {

    private final Map<K, List<V>> index;
    private final Observer observer;

    protected BehavioralMap(Collection<V> elements, Function<V, K> keyExtractor) {
        super(elements);
        this.observer = Observer.forClass(getClass());
        this.index = elements.stream()
                .collect(Collectors.groupingBy(keyExtractor,
                        Collectors.toUnmodifiableList()));
    }

    protected List<V> elementsForKey(K key) {
        observer.arguments("elementsForKey", i -> i.notNull(key, "key"))
                .observe();
        if (key == null) {
            return List.of();
        }
        return index.getOrDefault(key, List.of());
    }

    public boolean hasKey(K key) {
        return index.containsKey(key);
    }

    public Set<K> keys() {
        return Collections.unmodifiableSet(index.keySet());
    }
}
```

`elementsForKey` observes its argument and warns (via `.observe()`, not
`.throwWhenInvalid()`) on null key, returning an empty list gracefully. Domain
subclasses wrap it in their own typed collection. `BehavioralMap` inherits
`stream()`, `isEmpty()`, `size()`, `invariants()` from `BehavioralCollection`.

### ImageGallery (insects-api)

A concrete `BehavioralMap<InsectRankName, InsectImage>` in `InsectEntityCollections`.
Groups images by `parentName()` and exposes per-entity lookup returning `ImageCollection`.

```java
final class ImageGallery extends BehavioralMap<InsectRankName, InsectImage> {

    ImageGallery(Collection<InsectImage> images) {
        super(images, InsectImage::parentName);
    }

    public static ImageGallery of(Collection<InsectImage> images) {
        return new ImageGallery(images);
    }

    public static ImageGallery empty() {
        return new ImageGallery(List.of());
    }

    public ImageCollection forEntity(InsectRankName name) {
        return ImageCollection.of(elementsForKey(name));
    }
}
```

Constructed from a flat list of images. The gallery groups by `parentName()` internally,
so the caller (controller or query) just collects descendant images into a list — no
map-building at the call site. Templates call `gallery.forEntity(entity.name())` to get
the carousel images for each card.

### JTE component: `insects/cardImages.jte`

Shared carousel fragment. Parameters:

| Param     | Type               | Purpose                    |
|-----------|--------------------|----------------------------|
| `images`  | `List<InsectImage>` | images to rotate through  |
| `linkUrl` | `String`           | card click-through URL     |
| `alt`     | `String`           | image alt text             |

Renders the `<figure class="card-image" data-rotate="...">` block with `<img>` tags.
Caller wraps in `@if(!images.isEmpty())`. The rotation JavaScript stays at the bottom
of each page template (same pattern as today's `list.jte`).

### Controller changes

Each listing/detail method builds an `ImageGallery` by walking the hierarchy:

| Method           | Card entity      | Descendants gathered                              |
|------------------|------------------|---------------------------------------------------|
| `orders()`       | `InsectOrder`    | self + families -> genera -> species               |
| `orderDetail()`  | `InsectFamily`   | self + genera -> species                           |
| `families()`     | `InsectFamily`   | self + genera -> species                           |
| `familyDetail()` | `InsectGenus`    | self + species                                     |
| `genera()`       | `InsectGenus`    | self + species                                     |
| `genusDetail()`  | `InsectSpecies`  | images via `forParentName` directly                |

Private helper methods in the controller collect images for a given rank entity —
including images tagged directly to the entity itself plus all descendants:

- `imagesForOrder(InsectOrderName)` -> self + families -> genera -> species
- `imagesForFamily(InsectFamilyName)` -> self + genera -> species
- `imagesForGenus(InsectGenusName)` -> self + species

Each listing method loops over the page content, calls the appropriate helper to collect
all descendant images into a flat list, and wraps it as `ImageGallery.of(allImages)`.
The gallery handles the grouping internally.

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
