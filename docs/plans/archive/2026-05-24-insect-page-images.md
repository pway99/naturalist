# Insect Page Images Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add image carousels to every insect listing and detail page, with each card showing images gathered from all descendants in the taxonomic hierarchy.

**Architecture:** New `BehavioralMap<K,V>` kernel type extends `BehavioralCollection` with a keyed index. `ImageGallery` extends `BehavioralMap` in the insects domain. Controller walks the hierarchy to collect descendant images per card entity, wraps them in an `ImageGallery`, and passes to JTE templates that render a shared carousel component.

**Tech Stack:** Java 21 records, JTE templates, Spring MVC controllers, Observer framework

---

## Reference files

These files contain patterns and types referenced throughout the plan:

- `kernels/framework/src/main/java/com/naturalist/ddd/BehavioralCollection.java` — base class `BehavioralMap` extends
- `kernels/framework/src/main/java/com/naturalist/observability/Observer.java` — observer pattern for argument validation
- `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectEntityCollections.java` — where `ImageGallery` lives
- `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectImage.java` — the entity being collected
- `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java` — query methods for hierarchy walking
- `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` — controller to modify
- `domains/insects/insects-console/src/main/jte/insects/list.jte` — existing carousel pattern to extract

## Design note: two construction modes

`ImageGallery.of(flatList)` auto-groups by `InsectImage::parentName`. This works when
card entities match image parents (species cards). For higher-rank cards (order/family/genus
showing descendant images), the images' `parentName` is a species or genus name — not the
card entity name. Those pages use `ImageGallery.grouped(map)` where the controller
pre-computes the card-entity-to-images mapping.

`BehavioralMap` provides both constructors to support this.

---

### Task 1: BehavioralMap kernel type

**Files:**
- Create: `kernels/framework/src/main/java/com/naturalist/ddd/BehavioralMap.java`

No dedicated kernel test — first consumer (`ImageGallery`) tests it transitively per
kernel testing convention.

- [ ] **Step 1: Create BehavioralMap.java**

```java
package com.naturalist.ddd;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observable;
import com.naturalist.observability.Observer;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract base for keyed domain collections. Extends {@link BehavioralCollection}
 * with a secondary index that maps each element to a key, enabling grouped lookup
 * without raw {@code Map} types at the port boundary.
 *
 * <p>Two construction modes:
 * <ul>
 *   <li><b>Element-derived grouping</b> — a key extractor groups elements automatically.
 *       Use when the grouping key is a field on the element itself.</li>
 *   <li><b>Pre-computed grouping</b> — the caller supplies the mapping. Use when the
 *       grouping key is derived externally (e.g. hierarchical ancestor lookup).</li>
 * </ul>
 *
 * <p>{@link #elementsForKey(Object)} is {@code protected} — domain subclasses wrap it
 * in a typed method returning their own {@link BehavioralCollection} subclass.
 *
 * @param <K> the key type for the index
 * @param <V> the element type; must implement {@link Observable}
 */
public abstract class BehavioralMap<K, V extends Observable> extends BehavioralCollection<V> {

    private final Map<K, List<V>> index;
    private final Observer observer;

    /**
     * Element-derived grouping. Each element is assigned to a key via {@code keyExtractor}.
     */
    protected BehavioralMap(Collection<V> elements, Function<V, K> keyExtractor) {
        super(elements);
        this.observer = Observer.forClass(getClass());
        this.index = elements.stream()
                .collect(Collectors.groupingBy(keyExtractor,
                        Collectors.toUnmodifiableList()));
    }

    /**
     * Pre-computed grouping. The caller supplies the key-to-elements mapping; the flat
     * list is derived by flattening all values.
     */
    protected BehavioralMap(Map<K, ? extends Collection<V>> groups) {
        super(groups.values().stream().flatMap(Collection::stream).toList());
        this.observer = Observer.forClass(getClass());
        this.index = groups.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> List.copyOf(e.getValue())));
    }

    /**
     * Returns the elements grouped under {@code key}, or an empty list if the key is
     * absent. Observes the argument — a null key emits a warning metric via
     * {@link Observer#arguments} with {@code .observe()} (no throw) and returns empty.
     */
    protected List<V> elementsForKey(K key) {
        observer.arguments("elementsForKey", i -> i.notNull(key, "key"))
                .observe();
        if (key == null) {
            return List.of();
        }
        return index.getOrDefault(key, List.of());
    }

    public boolean hasKey(K key) {
        return key != null && index.containsKey(key);
    }

    public Set<K> keys() {
        return Collections.unmodifiableSet(index.keySet());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.notNull(this, c -> ((BehavioralMap<?, ?>) c).index, "index");
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add kernels/framework/src/main/java/com/naturalist/ddd/BehavioralMap.java
git commit -m "Add BehavioralMap kernel type — keyed index over BehavioralCollection"
```

---

### Task 2: ImageGallery in insects-api

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectEntityCollections.java`
- Create: `domains/insects/insects-api/src/test/java/com/naturalist/insects/ImageGalleryTest.java`

- [ ] **Step 1: Write the failing test**

Create `ImageGalleryTest.java`:

```java
package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.insects.InsectEntityCollections.ImageGallery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImageGalleryTest {

    private static final InsectSpeciesName SPECIES_A = InsectSpeciesName.of("species-a");
    private static final InsectSpeciesName SPECIES_B = InsectSpeciesName.of("species-b");
    private static final InsectGenusName GENUS_C = InsectGenusName.of("genus-c");
    private static final InsectOrderName ORDER_X = InsectOrderName.of("order-x");

    private static InsectImage image(InsectRankName parent, String filename) {
        return new InsectImage(
                InsectImageId.create(),
                parent,
                Instant.now(),
                FileName.of(filename));
    }

    @Test
    void ofGroupsByParentName() {
        var imgA1 = image(SPECIES_A, "a1.heic");
        var imgA2 = image(SPECIES_A, "a2.heic");
        var imgB1 = image(SPECIES_B, "b1.heic");

        ImageGallery gallery = ImageGallery.of(List.of(imgA1, imgA2, imgB1));

        assertThat(gallery.forEntity(SPECIES_A).stream().toList())
                .containsExactlyInAnyOrder(imgA1, imgA2);
        assertThat(gallery.forEntity(SPECIES_B).stream().toList())
                .containsExactly(imgB1);
        assertThat(gallery.size()).isEqualTo(3);
    }

    @Test
    void groupedUsesProvidedMapping() {
        var imgA1 = image(SPECIES_A, "a1.heic");
        var imgB1 = image(SPECIES_B, "b1.heic");

        // Group both images under ORDER_X (simulating hierarchy aggregation)
        ImageGallery gallery = ImageGallery.grouped(
                Map.of(ORDER_X, List.of(imgA1, imgB1)));

        assertThat(gallery.forEntity(ORDER_X).stream().toList())
                .containsExactlyInAnyOrder(imgA1, imgB1);
        // Species keys are NOT present — grouping is by order
        assertThat(gallery.forEntity(SPECIES_A).isEmpty()).isTrue();
        assertThat(gallery.size()).isEqualTo(2);
    }

    @Test
    void forEntityReturnsEmptyForUnknownKey() {
        ImageGallery gallery = ImageGallery.of(List.of(image(SPECIES_A, "a.heic")));

        ImageCollection result = gallery.forEntity(SPECIES_B);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void forEntityWithNullKeyReturnsEmptyWithoutThrowing() {
        ImageGallery gallery = ImageGallery.of(List.of(image(SPECIES_A, "a.heic")));

        ImageCollection result = gallery.forEntity(null);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void emptyGallery() {
        ImageGallery gallery = ImageGallery.empty();

        assertThat(gallery.isEmpty()).isTrue();
        assertThat(gallery.size()).isEqualTo(0);
        assertThat(gallery.forEntity(SPECIES_A).isEmpty()).isTrue();
    }

    @Test
    void hasKeyReflectsContent() {
        ImageGallery gallery = ImageGallery.of(List.of(image(GENUS_C, "c.heic")));

        assertThat(gallery.hasKey(GENUS_C)).isTrue();
        assertThat(gallery.hasKey(SPECIES_A)).isFalse();
        assertThat(gallery.hasKey(null)).isFalse();
    }

    @Test
    void streamReturnsAllElementsFlat() {
        var img1 = image(SPECIES_A, "a.heic");
        var img2 = image(SPECIES_B, "b.heic");

        ImageGallery gallery = ImageGallery.of(List.of(img1, img2));

        assertThat(gallery.stream().toList())
                .containsExactlyInAnyOrder(img1, img2);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl domains/insects/insects-api -Dtest=ImageGalleryTest -DfailIfNoTests=false`
Expected: FAIL — `ImageGallery` does not exist yet.

- [ ] **Step 3: Add ImageGallery to InsectEntityCollections**

Add this class inside the `InsectEntityCollections` interface, after the existing
`ImageCollection` class:

```java
    final class ImageGallery extends BehavioralMap<InsectRankName, InsectImage> {

        ImageGallery(Collection<InsectImage> images) {
            super(images, InsectImage::parentName);
        }

        ImageGallery(Map<InsectRankName, ? extends Collection<InsectImage>> groups) {
            super(groups);
        }

        /**
         * Construct a gallery that auto-groups images by their {@link InsectImage#parentName()}.
         * Use when the card entity matches the image parent rank (e.g. species cards).
         */
        public static ImageGallery of(Collection<InsectImage> images) {
            return new ImageGallery(images);
        }

        /**
         * Construct a gallery from a pre-computed grouping where the key is the card entity
         * name. Use when the card entity is a higher rank than the image parent (e.g. order
         * cards showing descendant species images).
         */
        public static ImageGallery grouped(Map<InsectRankName, ? extends Collection<InsectImage>> groups) {
            return new ImageGallery(groups);
        }

        public static ImageGallery empty() {
            return new ImageGallery(List.of());
        }

        /**
         * Returns the images grouped under the given entity name, or an empty
         * {@link ImageCollection} if no images exist for that entity.
         */
        public ImageCollection forEntity(InsectRankName name) {
            return ImageCollection.of(elementsForKey(name));
        }
    }
```

Add the required imports at the top of `InsectEntityCollections.java`:

```java
import com.naturalist.ddd.BehavioralMap;
import java.util.Map;
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl domains/insects/insects-api -Dtest=ImageGalleryTest`
Expected: PASS — all 7 tests green.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectEntityCollections.java
git add domains/insects/insects-api/src/test/java/com/naturalist/insects/ImageGalleryTest.java
git commit -m "Add ImageGallery — BehavioralMap grouping images by rank entity"
```

---

### Task 3: Extract cardImages.jte component

**Files:**
- Create: `domains/insects/insects-console/src/main/jte/insects/cardImages.jte`

- [ ] **Step 1: Create the shared carousel component**

```jte
@import com.naturalist.insects.InsectImage
@import java.util.List

@param List<InsectImage> images
@param String linkUrl
@param String alt

<a href="${linkUrl}" class="card-image-link">
    <figure class="card-image" data-rotate="${images.size() > 1}">
        @for(int i = 0; i < images.size(); i++)
            <img src="/insects/images/${images.get(i).resourceName().value()}"
                 alt="${alt}"
                 loading="lazy"
                 class="${i == 0 ? "card-image-active" : ""}">
        @endfor
    </figure>
</a>
```

- [ ] **Step 2: Commit**

```bash
git add domains/insects/insects-console/src/main/jte/insects/cardImages.jte
git commit -m "Extract cardImages.jte carousel component from species list"
```

---

### Task 4: Refactor species list to use ImageGallery and cardImages component

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`
- Modify: `domains/insects/insects-console/src/main/jte/insects/list.jte`

- [ ] **Step 1: Update the controller `list()` method**

In `InsectsController.java`, replace the `list()` method body. Change the image-gathering
loop to build an `ImageGallery` instead of `Map<InsectSpeciesName, List<InsectImage>>`.

Replace:
```java
Map<InsectSpeciesName, List<InsectImage>> imagesBySpecies = new LinkedHashMap<>();
```
and the image-gathering line inside the for loop:
```java
imagesBySpecies.put(
        species.name(),
        insectQuery.images().forParentName(species.name()).stream().toList());
```

With:
```java
List<InsectImage> allImages = new ArrayList<>();
```
and inside the for loop:
```java
allImages.addAll(insectQuery.images().forParentName(species.name()).stream().toList());
```

After the for loop, before model attributes:
```java
InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.of(allImages);
```

Replace the model attribute:
```java
model.addAttribute("imagesBySpecies", imagesBySpecies);
```
with:
```java
model.addAttribute("gallery", gallery);
```

Add imports at the top of `InsectsController.java`:
```java
import com.naturalist.insects.InsectEntityCollections;
import java.util.ArrayList;
```

- [ ] **Step 2: Update list.jte template**

Replace the `imagesBySpecies` param:
```jte
@param Map<InsectSpeciesName, List<InsectImage>> imagesBySpecies = Map.of()
```
with:
```jte
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
```

Add the import:
```jte
@import com.naturalist.insects.InsectEntityCollections
```

Remove the now-unused imports:
```jte
@import com.naturalist.insects.InsectImage
@import java.util.List
```

Replace the image-gathering line inside the for loop:
```jte
!{var images = imagesBySpecies.getOrDefault(s.name(), List.of());}
```
with:
```jte
!{var images = gallery.forEntity(s.name()).stream().toList();}
```

Replace the inline carousel block (the `@if(!images.isEmpty())` through `@endif` block,
lines 66-77) with:
```jte
@if(!images.isEmpty())
    @template.insects.cardImages(images = images, linkUrl = "/insects/" + s.name().value(), alt = commonName)
@endif
```

The `<script>` block at the bottom (lines 95-112) stays unchanged — it drives the
rotation for the `card-image` figures.

- [ ] **Step 3: Verify in browser**

Start the dev server and visit `/insects/species`. Confirm the carousel still rotates
through images on species cards (colias-eurytheme should show 6 rotating images).

- [ ] **Step 4: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git add domains/insects/insects-console/src/main/jte/insects/list.jte
git commit -m "Refactor species list to use ImageGallery and cardImages component"
```

---

### Task 5: Add controller image-gathering helpers

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`

These private helpers walk the taxonomy hierarchy and collect all descendant images
into a flat list. Each method includes images tagged directly to the entity itself
plus all descendants.

- [ ] **Step 1: Add the three helper methods**

Add these private methods to `InsectsController`:

```java
private List<InsectImage> imagesForGenus(InsectGenusName genusName) {
    List<InsectImage> images = new ArrayList<>(
            insectQuery.images().forParentName(genusName).stream().toList());
    for (var species : insectQuery.species().forGenusName(genusName).stream().toList()) {
        images.addAll(insectQuery.images().forParentName(species.name()).stream().toList());
    }
    return images;
}

private List<InsectImage> imagesForFamily(InsectFamilyName familyName) {
    List<InsectImage> images = new ArrayList<>(
            insectQuery.images().forParentName(familyName).stream().toList());
    for (var genus : insectQuery.genera().forFamilyName(familyName).stream().toList()) {
        images.addAll(imagesForGenus(genus.name()));
    }
    return images;
}

private List<InsectImage> imagesForOrder(InsectOrderName orderName) {
    List<InsectImage> images = new ArrayList<>(
            insectQuery.images().forParentName(orderName).stream().toList());
    for (var family : insectQuery.families().forOrderName(orderName).stream().toList()) {
        images.addAll(imagesForFamily(family.name()));
    }
    return images;
}
```

- [ ] **Step 2: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git commit -m "Add hierarchy image-gathering helpers to InsectsController"
```

---

### Task 6: Add images to orders listing and order detail pages

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`
- Modify: `domains/insects/insects-console/src/main/jte/insects/orders.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/order.jte`

- [ ] **Step 1: Update `orders()` controller method**

Add image gathering after the existing `orderPage` query. Build a grouped gallery
where each order name maps to all its descendant images:

```java
Map<InsectRankName, Collection<InsectImage>> imagesByOrder = new LinkedHashMap<>();
for (var order : orderPage.content()) {
    imagesByOrder.put(order.name(), imagesForOrder(order.name()));
}
InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByOrder);
model.addAttribute("gallery", gallery);
```

Add import to the controller (ArrayList was already added in Task 4):
```java
import java.util.Collection;
```

- [ ] **Step 2: Update orders.jte template**

Add imports:
```jte
@import com.naturalist.insects.InsectEntityCollections
```

Add the gallery param:
```jte
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
```

Inside the `@for(var o : orderPage.content())` loop, after the `</dl>` tag and before
the closing `</article>`, add:

```jte
                !{var images = gallery.forEntity(o.name()).stream().toList();}
                @if(!images.isEmpty())
                    @template.insects.cardImages(images = images, linkUrl = "/insects/orders/" + o.name().value(), alt = commonName)
                @endif
```

Add the carousel script before the closing `` `) ``:

```html
    <script>
        (function () {
            if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
                return;
            }
            var interval = 3500;
            document.querySelectorAll('.card-image[data-rotate]').forEach(function (el) {
                var imgs = el.querySelectorAll('img');
                if (imgs.length < 2) return;
                var i = 0;
                setInterval(function () {
                    imgs[i].classList.remove('card-image-active');
                    i = (i + 1) % imgs.length;
                    imgs[i].classList.add('card-image-active');
                }, interval);
            });
        }());
    </script>
```

- [ ] **Step 3: Update `orderDetail()` controller method**

Add image gathering after the `families` list is built. Each family card shows its
descendant images:

```java
Map<InsectRankName, Collection<InsectImage>> imagesByFamily = new LinkedHashMap<>();
for (var f : families) {
    imagesByFamily.put(f.name(), imagesForFamily(f.name()));
}
InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByFamily);
model.addAttribute("gallery", gallery);
```

- [ ] **Step 4: Update order.jte template**

Add imports:
```jte
@import com.naturalist.insects.InsectEntityCollections
```

Add the gallery param:
```jte
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
```

Inside the `@for(var f : families)` loop, after the `</dl>` tag and before `</article>`, add:

```jte
                        !{var images = gallery.forEntity(f.name()).stream().toList();}
                        @if(!images.isEmpty())
                            @template.insects.cardImages(images = images, linkUrl = "/insects/families/" + f.name().value(), alt = commonName)
                        @endif
```

Add the same carousel `<script>` block before the closing `` `) `` (same script as step 2).

- [ ] **Step 5: Verify in browser**

Visit `/insects/orders` — order cards should show rotating images from their descendants.
Visit `/insects/orders/lepidoptera` — family cards should show images.

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git add domains/insects/insects-console/src/main/jte/insects/orders.jte
git add domains/insects/insects-console/src/main/jte/insects/order.jte
git commit -m "Add image carousels to orders listing and order detail pages"
```

---

### Task 7: Add images to families listing and family detail pages

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`
- Modify: `domains/insects/insects-console/src/main/jte/insects/families.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/family.jte`

- [ ] **Step 1: Update `families()` controller method**

Add after the existing for loop that builds `orderByName`:

```java
Map<InsectRankName, Collection<InsectImage>> imagesByFamily = new LinkedHashMap<>();
for (var family : familyPage.content()) {
    imagesByFamily.put(family.name(), imagesForFamily(family.name()));
}
InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByFamily);
model.addAttribute("gallery", gallery);
```

- [ ] **Step 2: Update families.jte template**

Add imports:
```jte
@import com.naturalist.insects.InsectEntityCollections
```

Add the gallery param:
```jte
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
```

Inside the `@for(var f : familyPage.content())` loop, after the `</dl>` tag and before
`</article>`, add:

```jte
                !{var images = gallery.forEntity(f.name()).stream().toList();}
                @if(!images.isEmpty())
                    @template.insects.cardImages(images = images, linkUrl = "/insects/families/" + f.name().value(), alt = commonName)
                @endif
```

Add the carousel `<script>` block before the closing `` `) ``.

- [ ] **Step 3: Update `familyDetail()` controller method**

Add after the `genera` list is built:

```java
Map<InsectRankName, Collection<InsectImage>> imagesByGenus = new LinkedHashMap<>();
for (var g : genera) {
    imagesByGenus.put(g.name(), imagesForGenus(g.name()));
}
InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByGenus);
model.addAttribute("gallery", gallery);
```

- [ ] **Step 4: Update family.jte template**

Add imports:
```jte
@import com.naturalist.insects.InsectEntityCollections
```

Add the gallery param:
```jte
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
```

Inside the `@for(var g : genera)` loop, after `</dl>` and before `</article>`, add:

```jte
                        !{var images = gallery.forEntity(g.name()).stream().toList();}
                        @if(!images.isEmpty())
                            @template.insects.cardImages(images = images, linkUrl = "/insects/genera/" + g.name().value(), alt = commonName)
                        @endif
```

Add the carousel `<script>` block before the closing `` `) ``.

- [ ] **Step 5: Verify in browser**

Visit `/insects/families` — family cards should show descendant images.
Visit `/insects/families/pieridae` — genus cards should show images.

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git add domains/insects/insects-console/src/main/jte/insects/families.jte
git add domains/insects/insects-console/src/main/jte/insects/family.jte
git commit -m "Add image carousels to families listing and family detail pages"
```

---

### Task 8: Add images to genera listing and genus detail pages

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`
- Modify: `domains/insects/insects-console/src/main/jte/insects/genera.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/genus.jte`

- [ ] **Step 1: Update `genera()` controller method**

Add after the existing for loop that builds `familyByName`:

```java
Map<InsectRankName, Collection<InsectImage>> imagesByGenus = new LinkedHashMap<>();
for (var genus : genusPage.content()) {
    imagesByGenus.put(genus.name(), imagesForGenus(genus.name()));
}
InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByGenus);
model.addAttribute("gallery", gallery);
```

- [ ] **Step 2: Update genera.jte template**

Add imports:
```jte
@import com.naturalist.insects.InsectEntityCollections
```

Add the gallery param:
```jte
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
```

Inside the `@for(var g : genusPage.content())` loop, after `</dl>` and before `</article>`,
add:

```jte
                !{var images = gallery.forEntity(g.name()).stream().toList();}
                @if(!images.isEmpty())
                    @template.insects.cardImages(images = images, linkUrl = "/insects/genera/" + g.name().value(), alt = commonName)
                @endif
```

Add the carousel `<script>` block before the closing `` `) ``.

- [ ] **Step 3: Update `genusDetail()` controller method**

Add after the `members` list is built. For genus detail, species cards show only their
own images — build a grouped gallery per species:

```java
Map<InsectRankName, Collection<InsectImage>> imagesBySpecies = new LinkedHashMap<>();
for (var s : members) {
    imagesBySpecies.put(s.name(),
            insectQuery.images().forParentName(s.name()).stream().toList());
}
InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesBySpecies);
model.addAttribute("gallery", gallery);
```

- [ ] **Step 4: Update genus.jte template**

Add imports:
```jte
@import com.naturalist.insects.InsectEntityCollections
```

Add the gallery param:
```jte
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
```

Inside the `@for(var s : species)` loop, after `</dl>` and before `</article>`, add:

```jte
                        !{var images = gallery.forEntity(s.name()).stream().toList();}
                        @if(!images.isEmpty())
                            @template.insects.cardImages(images = images, linkUrl = "/insects/" + s.name().value(), alt = commonName)
                        @endif
```

Add the carousel `<script>` block before the closing `` `) ``.

- [ ] **Step 5: Verify in browser**

Visit `/insects/genera` — genus cards should show descendant images.
Visit `/insects/genera/colias` — species cards should show species-specific images.

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git add domains/insects/insects-console/src/main/jte/insects/genera.jte
git add domains/insects/insects-console/src/main/jte/insects/genus.jte
git commit -m "Add image carousels to genera listing and genus detail pages"
```
