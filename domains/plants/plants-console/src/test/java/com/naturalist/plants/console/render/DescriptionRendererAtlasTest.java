package com.naturalist.plants.console.render;

import com.naturalist.atlas.Atlas;
import com.naturalist.atlas.DomainId;
import com.naturalist.atlas.EntityRef;
import com.naturalist.atlas.MatchKind;
import com.naturalist.atlas.SearchHit;
import com.naturalist.atlas.SearchResults;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.plants.PlantName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance for M7 ({@code kernels/atlas/PLAN.md}). The renderer's binomial
 * pass becomes a binomial-italics-and-resolve pass when an {@link Atlas} and
 * {@link LinkResolver} are supplied: surface forms that resolve become
 * anchor tags, surface forms that miss stay italics-only.
 */
class DescriptionRendererAtlasTest {

    private static final PlantName PIPEVINE = PlantName.of("california-pipevine");
    private static final InsectSpeciesName HONEY_BEE = InsectSpeciesName.of("honey-bee");
    private static final CompoundName ARISTOLOCHIC_ACID = CompoundName.of("aristolochic-acid-i");

    @Test
    void surfaceFormThatResolvesIsWrappedInAnchor() {
        Atlas atlas = synthAtlas(Map.of(
                "Aristolochia californica", new EntityRef(new DomainId.Plants(), PIPEVINE)
        ));
        DescriptionRenderer renderer = new DescriptionRenderer(atlas, plantsLinkResolver());

        String rendered = renderer.render("The vine Aristolochia californica is the larval host.");

        assertThat(rendered).contains("<a href=\"/plants/california-pipevine\">"
                + "<em>Aristolochia californica</em></a>");
    }

    @Test
    void surfaceFormThatDoesNotResolveStaysItalicsOnly() {
        Atlas atlas = synthAtlas(Map.of()); // empty
        DescriptionRenderer renderer = new DescriptionRenderer(atlas, plantsLinkResolver());

        String rendered = renderer.render("The vine Aristolochia californica is the larval host.");

        assertThat(rendered).contains("<em>Aristolochia californica</em>");
        assertThat(rendered).doesNotContain("<a ");
    }

    @Test
    void resolverWithoutBuilderForTargetTypeFallsBackToItalicsOnly() {
        // Atlas resolves the surface form to a CompoundName, but the resolver
        // has no URL builder for CompoundName — graceful degradation, italics
        // only, no anchor.
        Atlas atlas = synthAtlas(Map.of(
                "Apis mellifera", new EntityRef(new DomainId.Plants(), ARISTOLOCHIC_ACID)
        ));
        LinkResolver plantsOnly = new LinkResolver(Map.of(
                PlantName.class, name -> "/plants/" + name.value()
        ));
        DescriptionRenderer renderer = new DescriptionRenderer(atlas, plantsOnly);

        String rendered = renderer.render("Apis mellifera visits the flower.");

        assertThat(rendered).contains("<em>Apis mellifera</em>");
        assertThat(rendered).doesNotContain("<a ");
    }

    @Test
    void emptyLinkResolverDegradesGracefully() {
        Atlas atlas = synthAtlas(Map.of(
                "Aristolochia californica", new EntityRef(new DomainId.Plants(), PIPEVINE)
        ));
        DescriptionRenderer renderer = new DescriptionRenderer(atlas, LinkResolver.empty());

        String rendered = renderer.render("Aristolochia californica is the host.");

        assertThat(rendered).contains("<em>Aristolochia californica</em>");
        assertThat(rendered).doesNotContain("<a ");
    }

    @Test
    void abbreviatedBinomialResolvesIndependentlyOfFullForm() {
        // The atlas is contributed with both the full binomial and the
        // abbreviated form for the same plant; both surfaces in prose link to
        // the same target.
        EntityRef ref = new EntityRef(new DomainId.Plants(), PIPEVINE);
        Atlas atlas = synthAtlas(Map.of(
                "Aristolochia californica", ref,
                "A. californica", ref
        ));
        DescriptionRenderer renderer = new DescriptionRenderer(atlas, plantsLinkResolver());

        String rendered = renderer.render(
                "Aristolochia californica is the larval host; A. californica leaves accumulate the toxin.");

        assertThat(rendered).contains(
                "<a href=\"/plants/california-pipevine\"><em>Aristolochia californica</em></a>");
        assertThat(rendered).contains(
                "<a href=\"/plants/california-pipevine\"><em>A. californica</em></a>");
    }

    @Test
    void mixedHitsAndMissesInOneParagraph() {
        Atlas atlas = synthAtlas(Map.of(
                "Aristolochia californica", new EntityRef(new DomainId.Plants(), PIPEVINE),
                "Apis mellifera", new EntityRef(new DomainId.Insects(), HONEY_BEE)
        ));
        LinkResolver resolver = new LinkResolver(Map.of(
                PlantName.class, name -> "/plants/" + name.value(),
                InsectSpeciesName.class, name -> "/insects/" + name.value()
        ));
        DescriptionRenderer renderer = new DescriptionRenderer(atlas, resolver);

        String rendered = renderer.render(
                "Aristolochia californica hosts Battus philenor; "
                        + "Apis mellifera visits Trifolium pratense.");

        assertThat(rendered).contains(
                "<a href=\"/plants/california-pipevine\"><em>Aristolochia californica</em></a>");
        assertThat(rendered).contains(
                "<a href=\"/insects/honey-bee\"><em>Apis mellifera</em></a>");
        // unresolved forms remain italics-only
        assertThat(rendered).contains("<em>Battus philenor</em>");
        assertThat(rendered).contains("<em>Trifolium pratense</em>");
    }

    @Test
    void noArgConstructorProducesItalicsOnlyEvenWhenAtlasWouldResolve() {
        // The atlas exists but the renderer was created without it — pure
        // legibility mode. Used by the M6 test class and by smoke tests that
        // don't care about cross-domain navigation.
        DescriptionRenderer renderer = new DescriptionRenderer();

        String rendered = renderer.render("Aristolochia californica is the host.");

        assertThat(rendered).contains("<em>Aristolochia californica</em>");
        assertThat(rendered).doesNotContain("<a ");
    }

    @Test
    void linkResolverWithEmptyMapReturnsEmptyOptional() {
        // Direct unit-level coverage of the resolver's miss path.
        LinkResolver resolver = LinkResolver.empty();
        Optional<String> url = resolver.urlFor(new EntityRef(new DomainId.Plants(), PIPEVINE));
        assertThat(url).isEmpty();
    }

    @Test
    void linkResolverWithBuilderReturnsBuiltUrl() {
        LinkResolver resolver = new LinkResolver(Map.of(
                PlantName.class, name -> "/plants/" + name.value()
        ));
        Optional<String> url = resolver.urlFor(new EntityRef(new DomainId.Plants(), PIPEVINE));
        assertThat(url).contains("/plants/california-pipevine");
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static LinkResolver plantsLinkResolver() {
        Map<Class<? extends EntityName>, Function<EntityName, String>> builders = new HashMap<>();
        builders.put(PlantName.class, name -> "/plants/" + name.value());
        builders.put(CompoundName.class, name -> "/chemistry/" + name.value());
        builders.put(InsectSpeciesName.class, name -> "/insects/" + name.value());
        return new LinkResolver(builders);
    }

    /**
     * Minimal {@link Atlas} backed by a fixed surface-form → target map. The
     * renderer treats a {@link SearchResults} containing exactly one
     * EXACT_TOKEN hit as a unique resolution; the synth atlas synthesises
     * that shape directly per matching surface form. Inverse SPI methods are
     * unused by these tests and return empty results.
     */
    private static Atlas synthAtlas(Map<String, EntityRef> aliases) {
        Map<String, EntityRef> copy = Map.copyOf(aliases);
        return new Atlas() {
            @Override public SearchResults search(String text) {
                EntityRef ref = copy.get(text);
                if (ref == null) {
                    return SearchResults.empty();
                }
                return SearchResults.of(List.of(new SearchHit(ref, text, MatchKind.EXACT_TOKEN)));
            }
            @Override public Set<DomainId> domainsReferencing(Class<? extends EntityName> referenceType) {
                return Set.of();
            }
            @Override public Map<DomainId, List<EntityRef>> findReferencesTo(EntityName target) {
                return Map.of();
            }
        };
    }
}
