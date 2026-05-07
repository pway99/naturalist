package com.naturalist.data;

import com.naturalist.ddd.Named;
import com.naturalist.observability.Constraints;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PagesTest {

    record Item(String name) implements Named<String> {
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

    @Test
    void streamWalksEveryPage() {
        ItemSource source = NaturalistDatabase.create().getNamed(ItemSource.class);
        IntStream.range(0, 7)
                .mapToObj(i -> new Item(String.format("item-%02d", i)))
                .forEach(source::insert);

        List<String> seen = Pages.stream(2, source::pageOf)
                .map(Item::name)
                .toList();

        assertThat(seen).containsExactly(
                "item-00", "item-01", "item-02", "item-03",
                "item-04", "item-05", "item-06");
    }

    @Test
    void streamOverEmptySourceYieldsNothing() {
        ItemSource source = NaturalistDatabase.create().getNamed(ItemSource.class);

        Stream<Item> stream = Pages.stream(10, source::pageOf);

        assertThat(stream.toList()).isEmpty();
    }

    @Test
    void streamIsLazy_loaderInvokedOnlyAsConsumed() {
        ItemSource source = NaturalistDatabase.create().getNamed(ItemSource.class);
        IntStream.range(0, 100)
                .mapToObj(i -> new Item(String.format("item-%03d", i)))
                .forEach(source::insert);

        AtomicInteger calls = new AtomicInteger();
        // 100 items at pageSize=10 → 10 pages. Limiting to 5 elements should
        // trigger only the first page (eager) plus exactly enough additional
        // pages to satisfy the limit. With pageSize=10, 5 elements come from
        // the first page alone — only the eager initial fetch should fire.
        Pages.stream(10, request -> {
            calls.incrementAndGet();
            return source.pageOf(request);
        }).limit(5).toList();

        assertThat(calls.get()).isEqualTo(1);
    }
}
