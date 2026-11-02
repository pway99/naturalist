package com.naturalist.insects;

import com.naturalist.naturalist.NaturalistName;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The read port for presentation: every call returns an {@link InsectViews} model.
 * Biology reads stay on {@link InsectQuery}.
 */
public interface InsectViewQuery {
    /**
     * The rank page for {@code name} as {@code viewer} sees it; {@code viewer} null means anonymous.
     * Empty when {@code InsectQuery.getByName(name)} is.
     */
    Optional<InsectViews.RankPage> rankPage(InsectRankName name, @Nullable NaturalistName viewer);
}
