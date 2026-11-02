package com.naturalist.insects;

import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
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

    /** A page of the species catalogue as cards; {@code viewer} null means anonymous (no card is collected). */
    Page<InsectViews.SpeciesCard> speciesCards(PageRequest request, @Nullable NaturalistName viewer);

    /**
     * A page over the species {@code viewer} has observed, ordered by species name; {@code collected}
     * is true on every card returned.
     */
    Page<InsectViews.SpeciesCard> collectedBy(NaturalistName viewer, PageRequest request);

    /** A page of orders as list cards (no parent). */
    Page<InsectViews.RankCard> orderCards(PageRequest request);

    /** A page of families as list cards; the parent is the order. */
    Page<InsectViews.RankCard> familyCards(PageRequest request);

    /** A page of genera as list cards; the parent is the family. */
    Page<InsectViews.RankCard> genusCards(PageRequest request);
}
