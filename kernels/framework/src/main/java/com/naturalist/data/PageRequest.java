package com.naturalist.data;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Page coordinates for a paged read against an {@link EntityRepository} or
 * {@link EntityQuery}.
 *
 * <p>{@code lookahead} is opt-in. With {@code lookahead == 0} the adapter
 * executes only the page query ({@code LIMIT pageSize + 1}) and reports a
 * binary "more exists?" via {@link Page#moreBeyondLookahead()}. With
 * {@code lookahead > 0} the adapter runs a second, thin, index-only probe
 * query that fills {@link Page#pagesAheadKnown()} up to {@code lookahead}.
 * Streaming consumers (catalog assembly, exports) use lookahead 0 to avoid
 * the extra round trip; console browse views opt in to render the
 * "page N of N+5+" horizon hint that nudges users toward filtering.
 *
 * <p>See {@code docs/plans/paged-queries-plan.md} Section 2 for the full
 * shape and renderer truth table.
 *
 * @param pageNumber 0-based; non-negative
 * @param pageSize   1..{@value MAX_PAGE_SIZE}
 * @param lookahead  0..{@value MAX_LOOKAHEAD}
 */
public record PageRequest(int pageNumber, int pageSize, int lookahead)
        implements ValueObject {

    public static final int MAX_PAGE_SIZE = 1000;

    public static final int MAX_LOOKAHEAD = 10;

    public static final int DEFAULT_CONSOLE_PAGE_SIZE = 25;

    public static final int DEFAULT_LOOKAHEAD = 5;

    public static PageRequest of(int pageNumber, int pageSize) {
        return new PageRequest(pageNumber, pageSize, 0);
    }

    public static PageRequest of(int pageNumber, int pageSize, int lookahead) {
        return new PageRequest(pageNumber, pageSize, lookahead);
    }

    public static PageRequest first(int pageSize) {
        return of(0, pageSize, 0);
    }

    public static PageRequest console(int pageNumber) {
        return of(pageNumber, DEFAULT_CONSOLE_PAGE_SIZE, DEFAULT_LOOKAHEAD);
    }

    public int offset() {
        return pageNumber * pageSize;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .atLeast(pageNumber, 0, "pageNumber")
                .inRange(pageSize, 1, MAX_PAGE_SIZE, "pageSize")
                .inRange(lookahead, 0, MAX_LOOKAHEAD, "lookahead");
    }
}
