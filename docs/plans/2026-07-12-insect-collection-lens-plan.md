# Insect Collection Lens + Header Restyle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the buried `?mine` link into a sticky, session-held "My collection" lens surfaced as a segmented toggle in a restyled header, and filter the species list + species-detail gallery to the naturalist's own observations/photos when the lens is on.

**Architecture:** The lens is an `HttpSession` boolean flipped by a CSRF-protected `POST /insects/collection-lens` endpoint; the insects controllers read it instead of the retired `?mine` param. The app's `NaturalistHeaderInterceptor` publishes `insectSection`/`collectionLens`/`requestUri` request attributes; the shared `page.jte` header renders a slim user area + the segmented toggle (only on Insects pages, only for a naturalist), staying within `spring-web` types.

**Tech Stack:** Java 21, Spring MVC / Spring Security 6, JTE, Pico CSS + `naturalist.css`.

## Global Constraints

- **Design source of truth:** `docs/plans/2026-07-12-insect-collection-lens-design.md`.
- **Retire `?mine`.** The `@RequestParam mine` and its link disappear; the session lens is the only source of truth.
- **Lens = session.** Key literal `"insects.collectionLens"` (Boolean), shared by convention between the app interceptor and the insects controller (documented, same as `"naturalist.currentNaturalistName"`). Read with `request.getSession(false)` (never create a session just to read).
- **Toggle visibility:** only when the path is under `/insects` AND the session is a logged-in **naturalist** (admin/anonymous never see it — gate on the `"naturalist.currentNaturalistName"` attribute being non-null, not merely `naturalistAuthenticated`).
- **Shared-template rule:** `page.jte` references only `spring-web` `RequestAttributes` — no Spring-Security / servlet / app types (it is compiled by every domain-console template test).
- **Open-redirect guard:** the toggle's `return` path is only honored when it `startsWith("/insects")` (and not the lens endpoint itself); otherwise redirect to `/insects/species`.
- **CSS cache-bust:** bump `naturalist.css?v=21` → `?v=22` in `page.jte` when the header ships visible changes (Task 4).
- **Builds:** Maven authorized for this execution. Console/template tasks build the reactor with `-am` (`mvn -q -pl apps/management-console -am test -Dtest=…`); the final task runs `mvn -q verify` from the repo root.
- **Commits:** stage + commit each task with the `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` footer.
- **Reuse (no new query):** my-photo filtering uses `insectQuery.fieldObservations().forNaturalist(me)` → `FieldObservationId` set → filter images by `observationId` membership. `FieldObservation.id()` / `.subject()`, `InsectImage.observationId()` (nullable) are the accessors.

---

## File Structure

- `domains/insects/insects-console/.../InsectsController.java` — **modify**: lens helper + endpoint; `list()` reads the session lens (retire `mine` param); `detail()` filters the gallery under the lens.
- `domains/insects/insects-console/src/main/jte/insects/list.jte` — **modify**: remove the old `?mine` toggle block; add the empty-state nudge.
- `domains/insects/insects-console/src/main/jte/insects/detail.jte` — **modify**: gallery empty-state under the lens (`lens` param).
- `apps/management-console/.../auth/NaturalistHeaderInterceptor.java` — **modify**: publish `insectSection`/`collectionLens`/`requestUri`.
- `apps/management-console/src/main/jte/layout/page.jte` — **modify**: slim user area + segmented toggle; bump `?v=`.
- `apps/management-console/src/main/resources/static/css/naturalist.css` — **modify**: `.site-user*`, `.site-logout`, `.collection-toggle`/`.lens-opt`, `.collection-empty`.
- Tests (app `@SpringBootTest`): rewrite `MyCollectionWebMvcTest` (session lens); add gallery + interceptor + header tests.

---

## Task 1: Lens session state + toggle endpoint (retire `?mine`)

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`
- Modify: `domains/insects/insects-console/src/main/jte/insects/list.jte`
- Test: `apps/management-console/src/test/java/com/naturalist/console/insects/MyCollectionWebMvcTest.java` (rewrite)
- Test: `apps/management-console/src/test/java/com/naturalist/console/insects/CollectionLensEndpointWebMvcTest.java` (new)

**Interfaces:**
- Consumes: existing `currentNaturalist(request)`; `insectQuery.fieldObservations().forNaturalist(...)`.
- Produces: `POST /insects/collection-lens` (params `on: boolean`, `return: String`); session attr `"insects.collectionLens"`; `collectionLensOn(HttpServletRequest)` helper.

- [ ] **Step 1: Rewrite the failing test for the session lens**

Replace `MyCollectionWebMvcTest.java` entirely (drop the `?mine` param; drive filtering via the session attribute):

```java
package com.naturalist.console.insects;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.console.auth.NaturalistPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class MyCollectionWebMvcTest {

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static UsernamePasswordAuthenticationToken as(String slug, String given) {
        var p = new NaturalistPrincipal(NaturalistName.of(slug), given, "{bcrypt}x");
        return new UsernamePasswordAuthenticationToken(p, "n/a", p.getAuthorities());
    }

    @Test
    void lensOn_filtersToObservedSpecies() throws Exception {
        mockMvc.perform(get("/insects/species")
                        .sessionAttr("insects.collectionLens", true)
                        .with(authentication(as("patrick-way", "Patrick"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("battus-philenor")))
                .andExpect(content().string(not(containsString("apis-mellifera"))));
    }

    @Test
    void lensOff_showsFullCatalog() throws Exception {
        mockMvc.perform(get("/insects/species").with(authentication(as("patrick-way", "Patrick"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("apis-mellifera")));
    }

    @Test
    void mineParam_isIgnored_afterRetirement() throws Exception {
        // ?mine=true must no longer filter — only the session lens does.
        mockMvc.perform(get("/insects/species").param("mine", "true")
                        .with(authentication(as("patrick-way", "Patrick"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("apis-mellifera")));
    }
}
```

Create `CollectionLensEndpointWebMvcTest.java` (the endpoint flips the session + safe redirect):

```java
package com.naturalist.console.insects;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.console.auth.NaturalistPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CollectionLensEndpointWebMvcTest {

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static UsernamePasswordAuthenticationToken patrick() {
        var p = new NaturalistPrincipal(NaturalistName.of("patrick-way"), "Patrick", "{bcrypt}x");
        return new UsernamePasswordAuthenticationToken(p, "n/a", p.getAuthorities());
    }

    @Test
    void toggleOn_persistsInSession_andFiltersNextRequest() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/insects/collection-lens").param("on", "true")
                        .param("return", "/insects/species")
                        .session(session).with(authentication(patrick())).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/species"));

        mockMvc.perform(get("/insects/species").session(session).with(authentication(patrick())))
                .andExpect(content().string(not(containsString("apis-mellifera"))));
    }

    @Test
    void toggle_rejectsOffsiteReturn() throws Exception {
        mockMvc.perform(post("/insects/collection-lens").param("on", "true")
                        .param("return", "https://evil.example/phish")
                        .with(authentication(patrick())).with(csrf()))
                .andExpect(redirectedUrl("/insects/species"));
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q -pl apps/management-console -am test -Dtest='MyCollectionWebMvcTest,CollectionLensEndpointWebMvcTest'`
Expected: FAIL — endpoint 404 / `?mine` still filters / `sessionAttr` ignored.

- [ ] **Step 3: Add the lens helper, constant, and endpoint to `InsectsController`**

Add near `CURRENT_NATURALIST_ATTRIBUTE`:

```java
    // Session flag toggled by POST /insects/collection-lens; read on every insects browse request.
    private static final String COLLECTION_LENS_ATTRIBUTE = "insects.collectionLens";

    private static boolean collectionLensOn(HttpServletRequest request) {
        var session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(COLLECTION_LENS_ATTRIBUTE));
    }

    private static String safeReturn(String returnTo) {
        return returnTo != null
                && returnTo.startsWith("/insects")
                && !returnTo.startsWith("/insects/collection-lens")
                ? returnTo : "/insects/species";
    }
```

Add the endpoint (alongside the other `@PostMapping`s):

```java
    @PostMapping("/collection-lens")
    String collectionLens(@RequestParam("on") boolean on,
                          @RequestParam(name = "return", required = false) String returnTo,
                          HttpServletRequest request) {
        if (currentNaturalist(request).isPresent()) {
            request.getSession(true).setAttribute(COLLECTION_LENS_ATTRIBUTE, on);
        }
        return "redirect:" + safeReturn(returnTo);
    }
```

- [ ] **Step 4: Switch `list()` from the `?mine` param to the session lens**

Change the handler signature and the `mine` read (everything else in the method stays identical):

```java
    @GetMapping("/species")
    String list(@RequestParam(defaultValue = "0") int page,
                HttpServletRequest request, Model model) {
        boolean mine = collectionLensOn(request);
```

(Remove the `@RequestParam(name = "mine", ...) boolean mine` parameter. The body's existing `if (mine && me.isPresent()) { … }` filter and `model.addAttribute("mine", mine && me.isPresent())` are unchanged — `mine` is now the session value.)

- [ ] **Step 5: Remove the old toggle block from `list.jte`**

Delete the `<div class="collection-toggle"> … </div>` block (the `@if(mine) … @else … @endif` links). Leave `@param boolean mine = false` in place — Task 4 uses it for the empty-state nudge. (The toggle now lives in the header, added in Task 4.)

- [ ] **Step 6: Run to verify it passes**

Run: `mvn -q -pl apps/management-console -am test -Dtest='MyCollectionWebMvcTest,CollectionLensEndpointWebMvcTest'`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add domains/insects/insects-console apps/management-console/src/test/java/com/naturalist/console/insects
git commit -m "feat(insects): session-held collection lens + toggle endpoint (retire ?mine)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: Filter the species-detail gallery to my photos

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` (`detail()`)
- Modify: `domains/insects/insects-console/src/main/jte/insects/detail.jte`
- Test: `apps/management-console/src/test/java/com/naturalist/console/insects/GalleryLensWebMvcTest.java` (new)

**Interfaces:**
- Consumes: `collectionLensOn(request)`, `currentNaturalist(request)` (Task 1); `insectQuery.fieldObservations().forNaturalist(me)`; `InsectImage.observationId()`; `FieldObservation.id()`.
- Produces: `detail()` passes `images` (filtered under lens) and a `lens` model attribute; `detail.jte` gains `@param boolean lens`.

- [ ] **Step 1: Write the failing test**

`battus-philenor` has a seeded catalog image (`pipevine-swallow-tail.HEIC`) with **no** `observationId` (owner-less). So under the lens, a naturalist who has only *observed* it (no photo of their own) should see **no** gallery photos. Create `GalleryLensWebMvcTest.java`:

```java
package com.naturalist.console.insects;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.console.auth.NaturalistPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class GalleryLensWebMvcTest {

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static UsernamePasswordAuthenticationToken patrick() {
        var p = new NaturalistPrincipal(NaturalistName.of("patrick-way"), "Patrick", "{bcrypt}x");
        return new UsernamePasswordAuthenticationToken(p, "n/a", p.getAuthorities());
    }

    @Test
    void lensOff_showsSharedCatalogPhoto() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor").with(authentication(patrick())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("pipevine-swallow-tail.HEIC")));
    }

    @Test
    void lensOn_hidesSharedPhoto_andShowsEmptyNote() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor")
                        .sessionAttr("insects.collectionLens", true)
                        .with(authentication(patrick())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("pipevine-swallow-tail.HEIC"))))
                .andExpect(content().string(containsString("No photos of yours here yet")));
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q -pl apps/management-console -am test -Dtest=GalleryLensWebMvcTest`
Expected: FAIL — the lens-on case still shows the shared photo and has no empty note.

- [ ] **Step 3: Filter the gallery in `detail()`**

Replace the `model.addAttribute("images", i.observations().stream().toList());` line with a lens-aware build, and add a `lens` attribute:

```java
        boolean lens = collectionLensOn(request);
        java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer = currentNaturalist(request);
        List<InsectImage> galleryImages = i.observations().stream().toList();
        if (lens && viewer.isPresent()) {
            java.util.Set<FieldObservationId> myObservationIds = insectQuery.fieldObservations()
                    .forNaturalist(viewer.get()).stream()
                    .map(FieldObservation::id)
                    .collect(java.util.stream.Collectors.toSet());
            galleryImages = galleryImages.stream()
                    .filter(img -> img.observationId() != null && myObservationIds.contains(img.observationId()))
                    .toList();
        }
        model.addAttribute("images", galleryImages);
        model.addAttribute("lens", lens && viewer.isPresent());
```

(`InsectImage`, `FieldObservation`, `FieldObservationId` are already imported via `com.naturalist.insects.*`.)

- [ ] **Step 4: Add the gallery empty-state to `detail.jte`**

Add `@param boolean lens = false` with the other params. After the existing `@if(!images.isEmpty()) … @endif` Photo Gallery block, add:

```jte
    @if(lens && images.isEmpty())
        <p class="collection-empty">No photos of yours here yet — add one below to start your collection.</p>
    @endif
```

- [ ] **Step 5: Run to verify it passes**

Run: `mvn -q -pl apps/management-console -am test -Dtest=GalleryLensWebMvcTest`
Expected: PASS. Also confirm the detail badge test still passes: `-Dtest=CollectedIndicatorWebMvcTest`.

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-console apps/management-console/src/test/java/com/naturalist/console/insects/GalleryLensWebMvcTest.java
git commit -m "feat(insects): species-detail gallery shows only my photos under the lens

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: Interceptor publishes header attributes

**Files:**
- Modify: `apps/management-console/src/main/java/com/naturalist/console/auth/NaturalistHeaderInterceptor.java`
- Test: `apps/management-console/src/test/java/com/naturalist/console/auth/NaturalistHeaderInterceptorTest.java` (extend)

**Interfaces:**
- Produces: request attributes `insectSection` (Boolean), `collectionLens` (Boolean), `requestUri` (String).

- [ ] **Step 1: Write the failing test** — add to `NaturalistHeaderInterceptorTest`:

```java
    @Test
    void publishesInsectSectionAndLensAndUri() {
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/insects/species");
        request.setQueryString("page=2");
        var session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("insects.collectionLens", true);
        request.setSession(session);

        interceptor.preHandle(request, new org.springframework.mock.web.MockHttpServletResponse(), new Object());

        org.assertj.core.api.Assertions.assertThat(request.getAttribute("insectSection")).isEqualTo(true);
        org.assertj.core.api.Assertions.assertThat(request.getAttribute("collectionLens")).isEqualTo(true);
        org.assertj.core.api.Assertions.assertThat(request.getAttribute("requestUri")).isEqualTo("/insects/species?page=2");
    }

    @Test
    void insectSectionFalse_offInsectsPath() {
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/chemistry");
        interceptor.preHandle(request, new org.springframework.mock.web.MockHttpServletResponse(), new Object());
        org.assertj.core.api.Assertions.assertThat(request.getAttribute("insectSection")).isEqualTo(false);
        org.assertj.core.api.Assertions.assertThat(request.getAttribute("collectionLens")).isEqualTo(false);
    }
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q -pl apps/management-console test -Dtest=NaturalistHeaderInterceptorTest`
Expected: FAIL — attributes not set.

- [ ] **Step 3: Publish the attributes** — in `preHandle`, before `return true;`:

```java
        request.setAttribute("insectSection", request.getRequestURI().startsWith("/insects"));
        var session = request.getSession(false);
        request.setAttribute("collectionLens",
                session != null && Boolean.TRUE.equals(session.getAttribute("insects.collectionLens")));
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        request.setAttribute("requestUri", queryString == null ? uri : uri + "?" + queryString);
```

- [ ] **Step 4: Run to verify it passes**

Run: `mvn -q -pl apps/management-console test -Dtest=NaturalistHeaderInterceptorTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add apps/management-console/src/main/java/com/naturalist/console/auth/NaturalistHeaderInterceptor.java apps/management-console/src/test/java/com/naturalist/console/auth/NaturalistHeaderInterceptorTest.java
git commit -m "feat(console): interceptor publishes insectSection/collectionLens/requestUri

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: Header restyle + segmented toggle + empty states + full verify

**Files:**
- Modify: `apps/management-console/src/main/jte/layout/page.jte`
- Modify: `apps/management-console/src/main/resources/static/css/naturalist.css`
- Modify: `domains/insects/insects-console/src/main/jte/insects/list.jte` (empty-state nudge)
- Modify: `apps/management-console/src/test/java/com/naturalist/console/auth/HeaderWebMvcTest.java` (assertions)
- Test: `apps/management-console/src/test/java/com/naturalist/console/insects/CollectionToggleWebMvcTest.java` (new)

**Interfaces:**
- Consumes: Task 3 attributes (`insectSection`, `collectionLens`, `requestUri`), the `"naturalist.currentNaturalistName"` attribute, the CSRF attributes; Task 1 endpoint `POST /insects/collection-lens`.

- [ ] **Step 1: Write the failing tests**

Create `CollectionToggleWebMvcTest.java`:

```java
package com.naturalist.console.insects;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.console.auth.NaturalistPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CollectionToggleWebMvcTest {

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static UsernamePasswordAuthenticationToken patrick() {
        var p = new NaturalistPrincipal(NaturalistName.of("patrick-way"), "Patrick", "{bcrypt}x");
        return new UsernamePasswordAuthenticationToken(p, "n/a", p.getAuthorities());
    }

    @Test
    void toggle_showsOnInsectsForNaturalist() throws Exception {
        mockMvc.perform(get("/insects/species").with(authentication(patrick())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/insects/collection-lens\"")))
                .andExpect(content().string(containsString("My collection")));
    }

    @Test
    void toggle_hiddenOffInsects() throws Exception {
        mockMvc.perform(get("/").with(authentication(patrick())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("action=\"/insects/collection-lens\""))));
    }

    @Test
    void toggle_hiddenForAdmin() throws Exception {
        mockMvc.perform(get("/insects/species").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("action=\"/insects/collection-lens\""))));
    }

    @Test
    void emptyCollection_showsNudge() throws Exception {
        var flora = new NaturalistPrincipal(NaturalistName.of("flora-mendez"), "Flora", "{bcrypt}x");
        var floraAuth = new UsernamePasswordAuthenticationToken(flora, "n/a", flora.getAuthorities());
        mockMvc.perform(get("/insects/species")
                        .sessionAttr("insects.collectionLens", true)
                        .with(authentication(floraAuth)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No insects in your collection yet")));
    }
}
```

> Note: `flora-mendez` is seeded with no observations. If another test in the suite has already observed a species for `flora` in the shared app context, use a different seeded naturalist with no observations, or assert against a naturalist you know stays empty — pick one and note it in the report.

Update `HeaderWebMvcTest` — the header no longer prints "Logged in as X"; it shows the name in a badge/name span. Change both existing assertions:
- `home_asNaturalist_showsNameAndLogout`: replace `containsString("Logged in as Patrick")` with `containsString("Patrick")` and keep `containsString("action=\"/logout\"")`.
- `home_asAdmin_showsAdministratorLabelAndLogout`: replace `containsString("Logged in as Administrator")` with `containsString("Administrator")`.

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q -pl apps/management-console -am test -Dtest='CollectionToggleWebMvcTest,HeaderWebMvcTest'`
Expected: FAIL — no toggle markup; header still says "Logged in as".

- [ ] **Step 3: Restyle the header + add the toggle in `page.jte`**

Replace the `<div class="site-user"> … </div>` block (inside the `@if(...naturalistAuthenticated...)`) with the actions group below, and read the extra attributes. The full replacement for the authenticated block:

```jte
        @if(_attrs != null && Boolean.TRUE.equals(_attrs.getAttribute("naturalistAuthenticated", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST)))
            !{var _name = (String) _attrs.getAttribute("naturalistDisplayName", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);}
            !{var _slug = (String) _attrs.getAttribute("naturalist.currentNaturalistName", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);}
            !{var _csrfParam = (String) _attrs.getAttribute("naturalistCsrfParam", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);}
            !{var _csrfValue = (String) _attrs.getAttribute("naturalistCsrfToken", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);}
            !{var _insectSection = Boolean.TRUE.equals(_attrs.getAttribute("insectSection", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST));}
            !{var _lensOn = Boolean.TRUE.equals(_attrs.getAttribute("collectionLens", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST));}
            !{var _returnTo = (String) _attrs.getAttribute("requestUri", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);}
            <div class="site-actions">
                @if(_insectSection && _slug != null)
                    <div class="collection-toggle" role="group" aria-label="Collection filter">
                        <form method="post" action="/insects/collection-lens" class="lens-form">
                            @if(_csrfParam != null)
                                <input type="hidden" name="${_csrfParam}" value="${_csrfValue}">
                            @endif
                            <input type="hidden" name="return" value="${_returnTo}">
                            <button type="submit" name="on" value="false" class="lens-opt ${!_lensOn ? "active" : ""}">All</button>
                            <button type="submit" name="on" value="true" class="lens-opt ${_lensOn ? "active" : ""}">My collection</button>
                        </form>
                    </div>
                @endif
                <div class="site-user">
                    @if(_name != null)
                        <span class="site-user-badge">${_name.substring(0, 1)}</span>
                        <span class="site-user-name">${_name}</span>
                    @endif
                    <form method="post" action="/logout" class="site-logout">
                        @if(_csrfParam != null)
                            <input type="hidden" name="${_csrfParam}" value="${_csrfValue}">
                        @endif
                        <button type="submit">Log out</button>
                    </form>
                </div>
            </div>
        @endif
```

Also bump the stylesheet cache-bust: change `href="/css/naturalist.css?v=21"` to `?v=22`.

- [ ] **Step 4: Add the CSS** — append to `naturalist.css`:

```css
.site-actions {
    display: flex;
    align-items: center;
    gap: 0.75rem;
}
.site-user {
    display: flex;
    align-items: center;
    gap: 0.4rem;
}
.site-user-badge {
    width: 1.6rem;
    height: 1.6rem;
    border-radius: 50%;
    background: var(--parchment-deep);
    color: var(--sepia-ink);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: 0.8rem;
}
.site-user-name {
    color: var(--sepia-ink);
    font-size: 0.9rem;
}
.site-logout {
    margin: 0;
}
.site-logout button {
    width: auto;
    background: none;
    border: none;
    color: var(--sepia-ink);
    font-size: 0.8rem;
    padding: 0.2rem 0.4rem;
    text-decoration: underline;
    cursor: pointer;
}
.collection-toggle {
    display: inline-flex;
    border: 1px solid var(--sepia-rule);
    border-radius: 999px;
    overflow: hidden;
}
.collection-toggle .lens-form {
    display: inline-flex;
    margin: 0;
}
.lens-opt {
    width: auto;
    background: var(--parchment);
    color: var(--sepia-ink);
    border: none;
    border-radius: 0;
    padding: 0.25rem 0.75rem;
    font-size: 0.8rem;
    cursor: pointer;
}
.lens-opt.active {
    background: var(--sepia-ink);
    color: var(--parchment);
}
.collection-empty {
    padding: 1rem;
    border: 1px dashed var(--sepia-rule);
    border-radius: 0.5rem;
    color: var(--sepia-ink);
}
```

- [ ] **Step 5: Add the species-list empty-state nudge to `list.jte`**

After the `<div class="entity-grid"> … </div>` (or where the grid renders), add:

```jte
    @if(mine && speciesList.isEmpty())
        <p class="collection-empty">No insects in your collection yet — turn off "My collection" and go make some observations.</p>
    @endif
```

- [ ] **Step 6: Run to verify it passes**

Run: `mvn -q -pl apps/management-console -am test -Dtest='CollectionToggleWebMvcTest,HeaderWebMvcTest,MyCollectionWebMvcTest,GalleryLensWebMvcTest,CollectedIndicatorWebMvcTest,ObserveWebMvcTest,CaptureLinksObservationWebMvcTest,NaturalistLoginWebMvcTest,AdminSecurityWebMvcTest'`
Expected: PASS (the toggle renders on insects for naturalists, hidden off-insects and for admin; header shows name + Log out; empty nudge renders; all prior collection/auth tests still green).

- [ ] **Step 7: Full verify**

Run: `mvn -q verify` (repo root)
Expected: BUILD SUCCESS across every module, including every domain-console template test (the shared `page.jte` change must not break chemistry/plants/insects/library template compilation — it references only `spring-web` types).

- [ ] **Step 8: Commit**

```bash
git add apps/management-console domains/insects/insects-console
git commit -m "feat(console): slim header user area + segmented My-collection lens toggle

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage** (against `2026-07-12-insect-collection-lens-design.md`):
- Sticky session lens + retire `?mine` → Task 1. ✔
- Toggle endpoint (CSRF, safe redirect) → Task 1. ✔
- Header: slim user area + segmented toggle, insects-only, naturalist-only → Task 4 (+ interceptor attrs Task 3). ✔
- Species-list lens filter → Task 1; gallery my-photos filter → Task 2. ✔
- Empty states (list nudge, gallery note) → Task 4 (list), Task 2 (gallery). ✔
- Logout restyle → Task 4. ✔
- Shared-template rule (spring-web only) → Task 4 uses only `RequestAttributes`. ✔
- Rank-page (order/family/genus) filtering → correctly OUT of scope, not implemented. ✔

**Placeholder scan:** none. The one soft spot — the empty-collection test naturalist (`flora-mendez`) potentially polluted by another test writing to the shared app context — is called out with a fallback instruction in Task 4 Step 1.

**Type consistency:** session key `"insects.collectionLens"` matches across the controller (`COLLECTION_LENS_ATTRIBUTE`), the interceptor, and the tests' `sessionAttr`. Attribute names `insectSection`/`collectionLens`/`requestUri` and `"naturalist.currentNaturalistName"` match between the interceptor (writer, Task 3) and `page.jte` (reader, Task 4). `POST /insects/collection-lens` params (`on`, `return`) match between the endpoint (Task 1), the tests, and the header form (Task 4). `collectionLensOn`/`currentNaturalist` helper names are consistent. The `lens` model attribute (Task 2) matches `detail.jte`'s `@param boolean lens`.
