package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.Aggregate;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Write-side consistency boundary for attaching a {@link Citation} to a
 * subject entity — the aggregate a {@link com.naturalist.data.Transaction}
 * persists atomically via {@link LibraryCommand#attributeCitation}.
 *
 * <p>Carries everything one attribution needs: the citation itself (which
 * may already exist — attribution is idempotent by {@code citation.name()}),
 * the {@link EntityRef} of the entity the citation supports, and an optional
 * human-readable note. The library domain owns idempotency for both writes
 * this aggregate implies (see {@code CitationAttributionTransaction} in
 * {@code library-core}) — a caller never needs to know whether the citation
 * or the association already exists.
 *
 * @param citation the citation to persist (insert-or-update by name)
 * @param subject  the entity this citation supports
 * @param note     optional context for why this citation is attached
 */
public record CitationAttribution(
        Citation citation,
        EntityRef subject,
        @Nullable String note
) implements Aggregate {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(citation, "citation")
                .valueObject(subject, "subject");
    }
}
