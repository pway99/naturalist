package com.naturalist.insects;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The larval stage of a holometabolous insect's life cycle.
 * <p>
 * Only present on species undergoing complete metamorphosis (Holometabola):
 * Neuroptera, Coleoptera, Diptera, Hymenoptera, Lepidoptera. Hemimetabolous
 * orders (Orthoptera, Hemiptera, Blattodea) produce nymphs, not larvae — their
 * immature stages are not modelled here.
 * <p>
 * {@code preyTargets} is empty for non-predatory larvae (e.g. lepidopteran
 * caterpillars that feed on plant tissue). {@code preyConsumption} gives a
 * quantitative rate where documented — for example, Chrysoperla larvae consume
 * 200+ aphids per week. {@code remarkableBehavior} captures field-notable
 * behaviour not covered by description or prey data (e.g. the debris-carrying
 * camouflage of lacewing larvae).
 */
public record LarvaStage(
        @Nullable String commonName,
        String description,
        @Nullable String preyConsumption,
        List<String> preyTargets,
        @Nullable String remarkableBehavior
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(this, LarvaStage::description, "description")
                .notNull(this, LarvaStage::preyTargets, "preyTargets");
    }
}
