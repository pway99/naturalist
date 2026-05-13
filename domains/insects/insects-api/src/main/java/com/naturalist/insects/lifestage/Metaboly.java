package com.naturalist.insects.lifestage;

import com.naturalist.insects.LifeStageKind;

import java.util.List;

/**
 * The developmental pattern a hexapod lineage follows: which life stages
 * exist, in what order. Declared once on the originating clade
 * (e.g. {@code Holometabola}) via {@link MetabolyTrait}; descendants
 * resolve it through {@link com.naturalist.clades.CladeTraversal#findTrait}.
 *
 * <p>Categories are Hexapoda-specific developmental vocabulary. Arachnids
 * use <i>anamorphic</i>/<i>epimorphic</i> growth, crustaceans use
 * <i>nauplius</i>/<i>zoea</i>/<i>megalopa</i> sequences, vertebrates have
 * entirely different concepts. Other kingdoms would model their own
 * developmental patterns separately rather than forcing a kingdom-neutral
 * abstraction here.
 *
 * <p>Sealed with stateless record permits — mirrors the shape of
 * {@link com.naturalist.clades.Clade}. Each permit derives equality from
 * its (zero) components, so {@code new Holometabolous().equals(new
 * Holometabolous())} is {@code true} and consumers can freely use a
 * Metaboly value as a map key, in sets, or in a {@code switch} selector.
 */
public sealed interface Metaboly
        permits Ametabolous, Hemimetabolous, Holometabolous {

    /** The life-stage sequence for this developmental pattern, in order. */
    List<LifeStageKind> stages();
}
