package com.naturalist.atlas;

import com.naturalist.atlas.AtlasContribution.Alias;
import com.naturalist.atlas.DomainId.Chemistry;
import com.naturalist.atlas.DomainId.Plants;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultAtlasTest {

    private static final EntityRef CALIFORNIA_PIPEVINE =
            new EntityRef(new Plants(), PlantName.of("california-pipevine"));
    private static final EntityRef CRIMSON_CLOVER =
            new EntityRef(new Plants(), PlantName.of("crimson-clover"));
    private static final EntityRef ARISTOLOCHIC_ACID =
            new EntityRef(new Chemistry(), CompoundName.of("aristolochic-acid"));

    private static AtlasContribution contribution(DomainId domain, Alias... aliases) {
        return new AtlasContribution() {
            @Override public DomainId domain() { return domain; }
            @Override public Stream<Alias> aliases() { return Stream.of(aliases); }
        };
    }

    @Test
    void exactSurfaceFormResolves() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("Aristolochia californica", CALIFORNIA_PIPEVINE)));

        assertThat(atlas.resolveAlias("Aristolochia californica"))
                .contains(CALIFORNIA_PIPEVINE);
    }

    @Test
    void slugFormResolves() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("california-pipevine", CALIFORNIA_PIPEVINE)));

        assertThat(atlas.resolveAlias("california-pipevine"))
                .contains(CALIFORNIA_PIPEVINE);
    }

    @Test
    void caseMismatchedSurfaceFormDoesNotResolve() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("Aristolochia californica", CALIFORNIA_PIPEVINE)));

        // Genus capitalisation matters: lower-case "aristolochia" is not the
        // genus and must not collide with "Aristolochia".
        assertThat(atlas.resolveAlias("aristolochia californica")).isEmpty();
        assertThat(atlas.resolveAlias("ARISTOLOCHIA CALIFORNICA")).isEmpty();
    }

    @Test
    void unknownSurfaceFormReturnsEmpty() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("crimson-clover", CRIMSON_CLOVER)));

        assertThat(atlas.resolveAlias("battus-philenor")).isEmpty();
    }

    @Test
    void nullTextReturnsEmpty() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("crimson-clover", CRIMSON_CLOVER)));

        assertThat(atlas.resolveAlias(null)).isEmpty();
    }

    @Test
    void emptyTextReturnsEmpty() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("crimson-clover", CRIMSON_CLOVER)));

        assertThat(atlas.resolveAlias("")).isEmpty();
    }

    @Test
    void emptyContributionListYieldsEmptyAtlas() {
        Atlas atlas = AtlasAssembly.from(List.of());

        assertThat(atlas.resolveAlias("anything")).isEmpty();
    }

    @Test
    void contributionWithNoAliasesIsLegal() {
        Atlas atlas = AtlasAssembly.from(contribution(new Plants()));

        assertThat(atlas.resolveAlias("anything")).isEmpty();
    }

    @Test
    void multipleContributionsCompose() {
        Atlas atlas = AtlasAssembly.from(
                contribution(new Plants(),
                        new Alias("Aristolochia californica", CALIFORNIA_PIPEVINE)),
                contribution(new Chemistry(),
                        new Alias("aristolochic acid", ARISTOLOCHIC_ACID)));

        assertThat(atlas.resolveAlias("Aristolochia californica"))
                .contains(CALIFORNIA_PIPEVINE);
        assertThat(atlas.resolveAlias("aristolochic acid"))
                .contains(ARISTOLOCHIC_ACID);
    }

    @Test
    void differentLengthSurfaceFormsCoexist() {
        // "Aristolochia" (genus) and "Aristolochia californica" (species) are
        // different keys; both store independently. The renderer is the layer
        // responsible for preferring the longer match in prose; the atlas
        // simply answers each lookup honestly.
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(),
                new Alias("Aristolochia", CALIFORNIA_PIPEVINE),
                new Alias("Aristolochia californica", CALIFORNIA_PIPEVINE)));

        assertThat(atlas.resolveAlias("Aristolochia")).contains(CALIFORNIA_PIPEVINE);
        assertThat(atlas.resolveAlias("Aristolochia californica")).contains(CALIFORNIA_PIPEVINE);
    }

    @Test
    void identicalSurfaceFormToSameTargetIsDeduplicated() {
        // Two contributions registering the same surface form to the same
        // target is benign — assembly should not fail.
        Atlas atlas = AtlasAssembly.from(
                contribution(new Plants(),
                        new Alias("california-pipevine", CALIFORNIA_PIPEVINE)),
                contribution(new Plants(),
                        new Alias("california-pipevine", CALIFORNIA_PIPEVINE)));

        assertThat(atlas.resolveAlias("california-pipevine"))
                .contains(CALIFORNIA_PIPEVINE);
    }

    @Test
    void identicalSurfaceFormToDifferentTargetsIsAssemblyError() {
        // Two contributions both claiming "Aristolochia" — one points at the
        // pipevine plant, another at the compound. Equal-length match → fail.
        AtlasContribution plants = contribution(new Plants(),
                new Alias("Aristolochia", CALIFORNIA_PIPEVINE));
        AtlasContribution chemistry = contribution(new Chemistry(),
                new Alias("Aristolochia", ARISTOLOCHIC_ACID));

        assertThatThrownBy(() -> AtlasAssembly.from(plants, chemistry))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aristolochia");
    }

    @Test
    void assemblyErrorMentionsBothTargets() {
        AtlasContribution plants = contribution(new Plants(),
                new Alias("Aristolochia", CALIFORNIA_PIPEVINE));
        AtlasContribution chemistry = contribution(new Chemistry(),
                new Alias("Aristolochia", ARISTOLOCHIC_ACID));

        assertThatThrownBy(() -> AtlasAssembly.from(plants, chemistry))
                .hasMessageContaining("california-pipevine")
                .hasMessageContaining("aristolochic-acid");
    }

    @Test
    void aliasInvariantsRejectBlankSurfaceForm() {
        Observer observer = Observer.forClass(DefaultAtlasTest.class);
        Alias alias = new Alias("   ", CALIFORNIA_PIPEVINE);

        InvariantObservation result = observer.forMethod("aliasInvariantsRejectBlankSurfaceForm")
                .observable(alias, "alias");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".surfaceForm"));
    }

    @Test
    void aliasInvariantsRejectNullTarget() {
        Observer observer = Observer.forClass(DefaultAtlasTest.class);
        Alias alias = new Alias("Aristolochia", null);

        InvariantObservation result = observer.forMethod("aliasInvariantsRejectNullTarget")
                .observable(alias, "alias");

        assertThat(result.violationNames())
                .anyMatch(n -> n.contains(".target"));
    }

    @Test
    void resolutionIsHotPathSafeAcrossManyLookups() {
        // Smoke test against the documented contract: resolution is a hash
        // lookup, not iteration over contributions. With 1000 aliases a tight
        // loop should complete in milliseconds.
        Alias[] aliases = new Alias[1000];
        for (int i = 0; i < aliases.length; i++) {
            aliases[i] = new Alias("alias-" + i, CRIMSON_CLOVER);
        }
        Atlas atlas = AtlasAssembly.from(contribution(new Plants(), aliases));

        Optional<EntityRef> last = Optional.empty();
        for (int i = 0; i < aliases.length; i++) {
            last = atlas.resolveAlias("alias-" + i);
        }

        assertThat(last).contains(CRIMSON_CLOVER);
    }
}
