package com.naturalist.clades;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class CladeTraversalTest {

    private record SampleTrait(String tag) implements Trait {
    }

    private record OtherTrait() implements Trait {
    }

    private static final Function<Clade, Set<Trait>> NO_TRAITS = c -> Set.of();

    @Test
    void ancestryWalksFromStartToRoot() {
        List<Clade> chain = CladeTraversal.ancestry(new Papilionidae());

        assertThat(chain).containsExactly(
                new Papilionidae(),
                new Lepidoptera(),
                new Holometabola(),
                new Insecta(),
                new Arthropoda(),
                new Animalia(),
                new Eukaryota());
    }

    @Test
    void ancestryFromRootContainsOnlyTheRoot() {
        assertThat(CladeTraversal.ancestry(new Eukaryota()))
                .containsExactly(new Eukaryota());
    }

    @Test
    void findTraitReturnsTheNearestDeclaredAncestor() {
        Map<Clade, Set<Trait>> declarations = Map.of(
                new Holometabola(), Set.of(new SampleTrait("metamorphosis")));

        Optional<SampleTrait> result = CladeTraversal.findTrait(
                new Papilionidae(),
                SampleTrait.class,
                c -> declarations.getOrDefault(c, Set.of()));

        assertThat(result).isPresent();
        assertThat(result.get().tag()).isEqualTo("metamorphosis");
    }

    @Test
    void findTraitReturnsEmptyWhenNoAncestorDeclaresIt() {
        Map<Clade, Set<Trait>> declarations = Map.of(
                new Holometabola(), Set.of(new SampleTrait("metamorphosis")));

        Optional<OtherTrait> result = CladeTraversal.findTrait(
                new Papilionidae(),
                OtherTrait.class,
                c -> declarations.getOrDefault(c, Set.of()));

        assertThat(result).isEmpty();
    }

    @Test
    void findTraitStopsAtTheFirstDeclaration() {
        // Two ancestors declare the trait; the nearest one wins.
        Map<Clade, Set<Trait>> declarations = Map.of(
                new Insecta(), Set.of(new SampleTrait("far")),
                new Lepidoptera(), Set.of(new SampleTrait("near")));

        Optional<SampleTrait> result = CladeTraversal.findTrait(
                new Papilionidae(),
                SampleTrait.class,
                c -> declarations.getOrDefault(c, Set.of()));

        assertThat(result).isPresent();
        assertThat(result.get().tag()).isEqualTo("near");
    }

    @Test
    void findTraitReturnsEmptyForAnEmptyDeclarationMap() {
        Optional<SampleTrait> result = CladeTraversal.findTrait(
                new Papilionidae(),
                SampleTrait.class,
                NO_TRAITS);

        assertThat(result).isEmpty();
    }

    /**
     * Models the recommended consumer idiom: each domain exposes a pure
     * {@code Function<Clade, Set<Trait>>} as a pattern-matching switch over
     * the sealed permits it cares about, returning {@code Set.of()} for the
     * rest. No registry, no startup wiring, no mutable state.
     */
    private static Set<Trait> sampleDomainTraits(Clade c) {
        return switch (c) {
            case Holometabola _ -> Set.of(new SampleTrait("complete-metamorphosis"));
            default -> Set.of();
        };
    }

    @Test
    void switchBasedTraitFunctionWorksAsTheConsumerWouldUseIt() {
        Optional<SampleTrait> fromPapilionidae = CladeTraversal.findTrait(
                new Papilionidae(),
                SampleTrait.class,
                CladeTraversalTest::sampleDomainTraits);

        assertThat(fromPapilionidae).isPresent();
        assertThat(fromPapilionidae.get().tag()).isEqualTo("complete-metamorphosis");

        // Insecta sits one node above Holometabola — the trait is not yet
        // declared at this level, so a search starting at Insecta returns
        // nothing rather than walking down into descendants.
        Optional<SampleTrait> fromInsecta = CladeTraversal.findTrait(
                new Insecta(),
                SampleTrait.class,
                CladeTraversalTest::sampleDomainTraits);

        assertThat(fromInsecta).isEmpty();
    }
}
