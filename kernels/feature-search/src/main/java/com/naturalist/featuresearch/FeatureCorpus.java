package com.naturalist.featuresearch;

import com.naturalist.ddd.EntityId;
import java.util.stream.Stream;

/**
 * Supplies the current set of a domain's features to an in-memory search. The
 * domain implements this by streaming its features (batched — never a per-element
 * select). The production adapter has its own index and ignores this SPI.
 */
@FunctionalInterface
public interface FeatureCorpus<ID extends EntityId> {

    Stream<Indexed<ID>> load();

    record Indexed<ID extends EntityId>(ID id, String value) {}
}
