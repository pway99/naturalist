package com.naturalist.data;

import com.naturalist.ddd.Named;
import com.naturalist.observability.Constraints;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kernel-level coverage of {@link TestEntitySource#pageOf} — the storage-side
 * primitive that drives the in-memory adapter and that the rdbms adapter must
 * reproduce. Exercises page boundaries, ordering determinism, and the full
 * lookahead truth table from {@code docs/plans/paged-queries-plan.md} Section 2.
 */
class TestEntitySourcePageTest {

    @RegisterExtension
    final NaturalistTestExtension db = NaturalistTestExtension.create();

    record Item(String name) implements Named<String> {
        @Override
        public String key() { return name; }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.notBlank(name, "name");
        }
    }

    public static class ItemSource extends TestEntitySource<String, Item> {
        public ItemSource(NaturalistDatabase database) {
            super(database);
        }
    }

    private ItemSource sourceWith(int n) {
        ItemSource source = db.getNamed(ItemSource.class);
        // names "item-00", "item-01", ... so ascending toString() order is also insertion order
        IntStream.range(0, n)
                .mapToObj(i -> new Item(String.format("item-%02d", i)))
                .forEach(source::insert);
        return source;
    }

    // ---------------------------------------------------------------------------------
    // Page boundaries
    // ---------------------------------------------------------------------------------

    @Test
    void emptySourceReturnsEmptyLastPage() {
        Page<Item> page = sourceWith(0).pageOf(PageRequest.of(0, 25));

        assertThat(page.content()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.pagesAheadKnown()).isZero();
        assertThat(page.moreBeyondLookahead()).isFalse();
    }

    @Test
    void requestPastEndReturnsEmptyPage() {
        Page<Item> page = sourceWith(5).pageOf(PageRequest.of(10, 2));

        assertThat(page.content()).isEmpty();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void singlePageUnderPageSizeIsLastPage() {
        Page<Item> page = sourceWith(3).pageOf(PageRequest.of(0, 25));

        assertThat(page.content()).hasSize(3);
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void lastPartialPageHasNoNext() {
        // 5 items, pageSize=2 → page 0 [00,01], page 1 [02,03], page 2 [04]
        Page<Item> page = sourceWith(5).pageOf(PageRequest.of(2, 2));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).name()).isEqualTo("item-04");
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void resultsAreOrderedAscendingByName() {
        // Insert deliberately out of order; expect ascending name() in the page.
        ItemSource source = db.getNamed(ItemSource.class);
        source.insert(new Item("zebra"));
        source.insert(new Item("apple"));
        source.insert(new Item("mango"));

        Page<Item> page = source.pageOf(PageRequest.of(0, 25));

        assertThat(page.content())
                .extracting(Item::name)
                .containsExactly("apple", "mango", "zebra");
    }

    // ---------------------------------------------------------------------------------
    // Lookahead = 0 → only the cheap "more exists?" signal is set.
    // ---------------------------------------------------------------------------------

    @Test
    void lookaheadZeroOnLastPageReportsNoMore() {
        Page<Item> page = sourceWith(5).pageOf(PageRequest.of(2, 2)); // last partial page

        assertThat(page.pagesAheadKnown()).isZero();
        assertThat(page.moreBeyondLookahead()).isFalse();
    }

    @Test
    void lookaheadZeroWithMoreRowsSetsMoreBeyondLookahead() {
        Page<Item> page = sourceWith(5).pageOf(PageRequest.of(0, 2));

        assertThat(page.pagesAheadKnown()).isZero();
        assertThat(page.moreBeyondLookahead()).isTrue();
    }

    // ---------------------------------------------------------------------------------
    // Lookahead > 0 → pagesAheadKnown is ceil(remaining/pageSize) capped at lookahead.
    // ---------------------------------------------------------------------------------

    @Test
    void lookaheadCountsExactPagesWhenWindowNotSaturated() {
        // 5 items, pageSize=2, page 0 → remaining=3 → ceil(3/2)=2 pages ahead
        Page<Item> page = sourceWith(5).pageOf(PageRequest.of(0, 2, 5));

        assertThat(page.pagesAheadKnown()).isEqualTo(2);
        assertThat(page.moreBeyondLookahead()).isFalse();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void lookaheadExactlyFillsWindow() {
        // 12 items, pageSize=2, lookahead=5, page 0 → remaining=10
        // ceil(10/2)=5 pages ahead, exactly the lookahead → not saturated past
        Page<Item> page = sourceWith(12).pageOf(PageRequest.of(0, 2, 5));

        assertThat(page.pagesAheadKnown()).isEqualTo(5);
        assertThat(page.moreBeyondLookahead()).isFalse();
    }

    @Test
    void lookaheadSaturatesWhenExtraRowExistsBeyondWindow() {
        // 13 items, pageSize=2, lookahead=5, page 0 → remaining=11
        // ceil(11/2)=6 → capped at 5; moreBeyondLookahead=true (11 > 10)
        Page<Item> page = sourceWith(13).pageOf(PageRequest.of(0, 2, 5));

        assertThat(page.pagesAheadKnown()).isEqualTo(5);
        assertThat(page.moreBeyondLookahead()).isTrue();
    }

    @Test
    void lookaheadOnLastPageReportsNoMore() {
        // last page of 5-item source with lookahead=5
        Page<Item> page = sourceWith(5).pageOf(PageRequest.of(2, 2, 5));

        assertThat(page.pagesAheadKnown()).isZero();
        assertThat(page.moreBeyondLookahead()).isFalse();
        assertThat(page.hasNext()).isFalse();
    }
}
