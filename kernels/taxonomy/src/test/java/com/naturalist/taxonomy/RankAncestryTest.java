package com.naturalist.taxonomy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RankAncestryTest {

    /** A fake rank name with a slug; equality by value (record) so the cycle guard works. */
    record Rank(String value) implements RankName {
        @Override public LinealRank rank() { return LinealRank.GENUS; } // unused here
    }

    // species -> genus -> family; family has no parent (top of this fake chain)
    private static final Map<Rank, Rank> PARENT = Map.of(
            new Rank("empoasca"), new Rank("cicadellidae"),
            new Rank("cicadellidae"), new Rank("hemiptera"));

    private static Optional<Rank> parentOf(Rank r) { return Optional.ofNullable(PARENT.get(r)); }

    @Test
    void ancestryIsSubjectFirstThenAncestorsAscending() {
        List<Rank> chain = RankAncestry.ancestry(new Rank("empoasca"), RankAncestryTest::parentOf);
        assertThat(chain).containsExactly(
                new Rank("empoasca"), new Rank("cicadellidae"), new Rank("hemiptera"));
    }

    @Test
    void ancestryStopsAtAMissingLink() {
        // "unknown" has no parent mapping -> chain is just itself
        assertThat(RankAncestry.ancestry(new Rank("unknown"), RankAncestryTest::parentOf))
                .containsExactly(new Rank("unknown"));
    }

    @Test
    void ancestryIsCycleGuarded() {
        // a -> b -> a would loop forever without the guard
        Map<Rank, Rank> cyclic = Map.of(new Rank("a"), new Rank("b"), new Rank("b"), new Rank("a"));
        List<Rank> chain = RankAncestry.ancestry(new Rank("a"), r -> Optional.ofNullable(cyclic.get(r)));
        assertThat(chain).containsExactly(new Rank("a"), new Rank("b")); // stops before repeating "a"
    }

    @Test
    void inheritedTagsEachAttributeWithItsSourceRankInAncestryOrder() {
        // attributes: genus "empoasca" has [G1]; family "cicadellidae" has [F1, F2]
        Map<Rank, List<String>> attrs = Map.of(
                new Rank("empoasca"), List.of("G1"),
                new Rank("cicadellidae"), List.of("F1", "F2"));
        List<RankAncestry.AtRank<Rank, String>> tagged = RankAncestry.inherited(
                new Rank("empoasca"), RankAncestryTest::parentOf,
                r -> attrs.getOrDefault(r, List.of()));
        assertThat(tagged).containsExactly(
                new RankAncestry.AtRank<>("G1", new Rank("empoasca")),
                new RankAncestry.AtRank<>("F1", new Rank("cicadellidae")),
                new RankAncestry.AtRank<>("F2", new Rank("cicadellidae")));
        // hemiptera contributed nothing and simply doesn't appear
    }
}
