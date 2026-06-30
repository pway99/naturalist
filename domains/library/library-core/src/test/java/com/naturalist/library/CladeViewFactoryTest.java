package com.naturalist.library;

import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CladeViewFactoryTest {

    private final CladeViewFactory factory = new CladeViewFactory();

    @Test
    void ancestryIsRootToParentOrder() {
        Optional<CladeView> result = factory.buildBySlug("lepidoptera");

        assertThat(result).isPresent();
        CladeView view = result.get();
        assertThat(view.ancestry().stream().map(CladeStep::cladeSlug).toList())
                .containsExactly(
                        "eukaryota", "animalia", "arthropoda",
                        "insecta", "holometabola");
    }

    @Test
    void subjectIsExcludedFromAncestry() {
        Optional<CladeView> result = factory.buildBySlug("lepidoptera");

        assertThat(result).isPresent();
        assertThat(result.get().ancestry().stream().map(CladeStep::cladeSlug))
                .doesNotContain("lepidoptera");
    }

    @Test
    void subjectCarriesCorrectSlugAndDisplayName() {
        Optional<CladeView> result = factory.buildBySlug("lepidoptera");

        assertThat(result).isPresent();
        assertThat(result.get().subject().cladeSlug()).isEqualTo("lepidoptera");
        assertThat(result.get().subject().displayName()).isEqualTo("Lepidoptera");
    }

    @Test
    void rankedCladesCarryTheirLinealRank() {
        Optional<CladeView> result = factory.buildBySlug("lepidoptera");

        assertThat(result).isPresent();
        CladeView view = result.get();

        assertThat(stepBySlug(view, "animalia").rank())
                .isEqualTo(Optional.of(LinealRank.KINGDOM));
        assertThat(stepBySlug(view, "arthropoda").rank())
                .isEqualTo(Optional.of(LinealRank.PHYLUM));
        assertThat(stepBySlug(view, "insecta").rank())
                .isEqualTo(Optional.of(LinealRank.CLASS));
        assertThat(view.subject().rank())
                .isEqualTo(Optional.of(LinealRank.ORDER));
    }

    @Test
    void ranklessCladesYieldEmptyRank() {
        Optional<CladeView> result = factory.buildBySlug("lepidoptera");

        assertThat(result).isPresent();
        CladeView view = result.get();

        assertThat(stepBySlug(view, "eukaryota").rank()).isEmpty();
        assertThat(stepBySlug(view, "holometabola").rank()).isEmpty();
    }

    @Test
    void papilionoideaIsRankless() {
        Optional<CladeView> result = factory.buildBySlug("papilionoidea");

        assertThat(result).isPresent();
        assertThat(result.get().subject().rank()).isEmpty();
    }

    @Test
    void childrenAreDirectDescendantsSortedByName() {
        Optional<CladeView> result = factory.buildBySlug("insecta");

        assertThat(result).isPresent();
        assertThat(result.get().children().stream().map(CladeStep::cladeSlug).toList())
                .containsExactly("blattodea", "hemiptera", "holometabola");
    }

    @Test
    void rootCladeHasEmptyAncestry() {
        Optional<CladeView> result = factory.buildBySlug("eukaryota");

        assertThat(result).isPresent();
        assertThat(result.get().ancestry()).isEmpty();
    }

    @Test
    void unknownSlugReturnsEmpty() {
        Optional<CladeView> result = factory.buildBySlug("unobtainium");

        assertThat(result).isEmpty();
    }

    @Test
    void treeRootIsEukaryota() {
        CladeQuery.CladeTreeNode tree = factory.buildTree();

        assertThat(tree.slug()).isEqualTo("eukaryota");
        assertThat(tree.rank()).isEmpty();
    }

    @Test
    void treeContainsNestedChildren() {
        CladeQuery.CladeTreeNode tree = factory.buildTree();

        // Eukaryota → Animalia → Arthropoda → Insecta
        assertThat(tree.children()).hasSize(1);
        CladeQuery.CladeTreeNode animalia = tree.children().getFirst();
        assertThat(animalia.slug()).isEqualTo("animalia");
        assertThat(animalia.rank()).isEqualTo(Optional.of(LinealRank.KINGDOM));
    }

    private CladeStep stepBySlug(CladeView view, String slug) {
        return view.ancestry().stream()
                .filter(s -> s.cladeSlug().equals(slug))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no step with slug: " + slug));
    }
}
