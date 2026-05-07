package com.naturalist.data;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.function.Consumer;

/**
 * A single page of paged read results.
 *
 * <p>Carries no total count. Horizon information comes from the optional
 * lookahead probe driven by {@link PageRequest#lookahead()} — see Section 2
 * of {@code docs/plans/paged-queries-plan.md} for the full semantics and
 * renderer truth table.
 *
 * <p>Field semantics:
 * <ul>
 *   <li>{@code pagesAheadKnown} — number of additional full pages confirmed
 *       by the lookahead probe. {@code 0} when the request had
 *       {@code lookahead == 0} (no probe was issued), else
 *       {@code 0..lookahead}.</li>
 *   <li>{@code moreBeyondLookahead} — {@code true} if at least one row
 *       exists past the lookahead window. When the request had
 *       {@code lookahead == 0}, this collapses to "at least one more row
 *       exists" — i.e. the cheap {@code hasNext} signal recovered from the
 *       page query's {@code pageSize + 1} fetch.</li>
 * </ul>
 *
 * @param <T> the row type
 */
public record Page<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        int pagesAheadKnown,
        boolean moreBeyondLookahead
) implements ValueObject {

    public Page {
        if (content != null) {
            content = List.copyOf(content);
        }
    }

    public boolean hasNext() {
        return pagesAheadKnown > 0 || moreBeyondLookahead;
    }

    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(content, "content")
                .atLeast(pageNumber, 0, "pageNumber")
                .inRange(pageSize, 1, PageRequest.MAX_PAGE_SIZE, "pageSize")
                .inRange(pagesAheadKnown, 0, PageRequest.MAX_LOOKAHEAD, "pagesAheadKnown");
    }
}
