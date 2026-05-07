package com.naturalist.data;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Streaming helpers over the paged-read contract.
 *
 * <p>Used by consumers that need to walk an entire catalog without loading it
 * into memory all at once: catalog assembly, bulk exports, anything whose
 * job is iteration rather than browsing. Pages are loaded lazily; each page
 * uses {@code lookahead == 0} so the adapter never pays for horizon
 * information the consumer does not consult.
 */
public final class Pages {

    private Pages() {
    }

    /**
     * Lazy stream over every entity reachable through {@code loader}, paging
     * forward {@code pageSize} rows at a time. The first page is requested
     * eagerly to let the adapter signal failure up front; subsequent pages
     * load on demand as the stream is consumed.
     *
     * <p>Pick {@code pageSize} for the consumer, not for the adapter — a
     * catalog-assembly streamer pushing into Solr typically uses 1000;
     * a smaller throughput-bounded consumer can use less. The kernel
     * caps {@code pageSize} at {@link PageRequest#MAX_PAGE_SIZE}.
     *
     * @param pageSize rows per page
     * @param loader   page-fetch callback, typically {@code repository::getPage}
     *                 or {@code query::findPage}
     * @param <E>      the entity type
     */
    public static <E> Stream<E> stream(int pageSize, Function<PageRequest, Page<E>> loader) {
        Page<E> first = loader.apply(PageRequest.first(pageSize));
        return Stream.iterate(
                        first,
                        Objects::nonNull,
                        page -> page.hasNext()
                                ? loader.apply(PageRequest.of(page.pageNumber() + 1, pageSize))
                                : null)
                .flatMap(page -> page.content().stream());
    }
}
