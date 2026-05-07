package com.naturalist.data;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.naturalist.data.PageRequest.MAX_LOOKAHEAD;
import static com.naturalist.data.PageRequest.MAX_PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

class PageTest {

    private static final Observer observer = Observer.forClass(PageTest.class);

    private static Page<String> page(int pagesAheadKnown, boolean moreBeyondLookahead) {
        return new Page<>(List.of("a", "b"), 0, 25, pagesAheadKnown, moreBeyondLookahead);
    }

    @Test
    void wellFormedPagePassesInvariants() {
        InvariantObservation result = observer.forMethod("wellFormedPagePassesInvariants")
                .observable(page(3, false), "page");

        assertThat(result.violations()).isEmpty();
    }

    // ---------------------------------------------------------------------------------
    // hasNext truth table — see plan Section 2 renderer table.
    // ---------------------------------------------------------------------------------

    @Test
    void lastPageHasNoNext() {
        assertThat(page(0, false).hasNext()).isFalse();
    }

    @Test
    void zeroLookaheadWithMoreRowsHasNext() {
        // lookahead=0 mode: pagesAheadKnown=0, moreBeyondLookahead carries hasNext.
        assertThat(page(0, true).hasNext()).isTrue();
    }

    @Test
    void exactHorizonInsideLookaheadHasNext() {
        // lookahead>0 mode, exactly N more pages exist (window not saturated).
        assertThat(page(3, false).hasNext()).isTrue();
    }

    @Test
    void saturatedLookaheadHasNext() {
        // lookahead=5 fully saturated AND more rows exist past the window.
        assertThat(page(5, true).hasNext()).isTrue();
    }

    // ---------------------------------------------------------------------------------
    // Invariant rejection
    // ---------------------------------------------------------------------------------

    @Test
    void nullContentViolatesInvariants() {
        InvariantObservation result = observer.forMethod("nullContentViolatesInvariants")
                .observable(new Page<String>(null, 0, 25, 0, false), "page");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".content"));
    }

    @Test
    void negativePageNumberViolatesInvariants() {
        InvariantObservation result = observer.forMethod("negativePageNumberViolatesInvariants")
                .observable(new Page<>(List.of(), -1, 25, 0, false), "page");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".pageNumber"));
    }

    @Test
    void zeroPageSizeViolatesInvariants() {
        InvariantObservation result = observer.forMethod("zeroPageSizeViolatesInvariants")
                .observable(new Page<>(List.of(), 0, 0, 0, false), "page");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".pageSize"));
    }

    @Test
    void oversizedPageSizeViolatesInvariants() {
        InvariantObservation result = observer.forMethod("oversizedPageSizeViolatesInvariants")
                .observable(new Page<>(List.of(), 0, MAX_PAGE_SIZE + 1, 0, false), "page");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".pageSize"));
    }

    @Test
    void negativePagesAheadKnownViolatesInvariants() {
        InvariantObservation result = observer.forMethod("negativePagesAheadKnownViolatesInvariants")
                .observable(new Page<>(List.of(), 0, 25, -1, false), "page");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".pagesAheadKnown"));
    }

    @Test
    void oversizedPagesAheadKnownViolatesInvariants() {
        InvariantObservation result = observer.forMethod("oversizedPagesAheadKnownViolatesInvariants")
                .observable(new Page<>(List.of(), 0, 25, MAX_LOOKAHEAD + 1, false), "page");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".pagesAheadKnown"));
    }

    // ---------------------------------------------------------------------------------
    // Defensive copy — caller mutation must not leak into the page.
    // ---------------------------------------------------------------------------------

    @Test
    void contentIsDefensivelyCopied() {
        List<String> mutable = new ArrayList<>(List.of("a", "b"));
        Page<String> page = new Page<>(mutable, 0, 25, 0, false);

        mutable.add("smuggled");

        assertThat(page.content()).containsExactly("a", "b");
    }

    @Test
    void emptyContentIsValid() {
        InvariantObservation result = observer.forMethod("emptyContentIsValid")
                .observable(new Page<>(List.of(), 0, 25, 0, false), "page");

        assertThat(result.violations()).isEmpty();
        assertThat(new Page<>(List.of(), 0, 25, 0, false).isEmpty()).isTrue();
    }
}
