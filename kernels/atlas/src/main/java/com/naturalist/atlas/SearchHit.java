package com.naturalist.atlas;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * A single search-result entry — the typed reference plus the diagnostic
 * context the UI needs to explain the match.
 * <p>
 * {@code matchedToken} is the contributed surface form that the search
 * query matched (after normalisation), not the raw query text. It lets
 * the search results page render a "matched: {token}" hint when the token
 * is not the slug — a reader who searches {@code "pipevine"} and lands on
 * a hit whose slug is {@code "california-pipevine"} sees why.
 *
 * @param target       the entity reference this hit points at
 * @param matchedToken the contributed token the query matched
 * @param kind         the shape of the match (exact slug, exact token, prefix)
 */
public record SearchHit(EntityRef target, String matchedToken, MatchKind kind)
        implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .valueObject(target, "target")
                .notBlank(matchedToken, "matchedToken")
                .notNull(kind, "kind");
    }
}
