package com.naturalist.fieldnotes;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * A four-level description of any catalog entity, embodying Durrell's principle:
 * the same truth at different resolutions.
 * <p>
 * Inspired by Gerald Durrell's <i>The Amateur Naturalist</i> (Alfred A. Knopf, 1983)
 * — the recognition that ecological truth is not a single statement but a layered
 * understanding, each level wholly accurate and none contradicting any other.
 * A child grows into the deeper levels as understanding deepens; an adult returns
 * to the preschool level to rediscover wonder.
 * <p>
 * <b>Level conventions:</b>
 * <ul>
 *   <li><b>Preschool (age &lt;6)</b> — wonder and direct sensory observation.
 *       What does it look like? What does it do that you can see? No mechanisms,
 *       no jargon. Pure observation and delight.</li>
 *   <li><b>Elementary (age 6–11)</b> — simple ecological relationship.
 *       Who eats whom? How does it help the garden? Cause and effect without
 *       biochemistry or Latin.</li>
 *   <li><b>Secondary (age 12–18)</b> — mechanism and process. Life cycle,
 *       ecological guild function, observable evidence of activity. Scientific
 *       names introduced in context.</li>
 *   <li><b>University (age 18+)</b> — precise scientific terminology. Taxonomic
 *       placement, physiological mechanisms, quantitative ecological role,
 *       Oak Vista management relevance.</li>
 * </ul>
 * <p>
 * All four levels are always simultaneously true. The descriptions are additive,
 * not contradictory. A preschool description of a parasitoid wasp focuses on its
 * role as a garden protector; the university description adds the koinobiont/idiobiont
 * distinction and oviposition strategy. Both describe the same organism faithfully.
 */
public record Description(
        String preschool,
        String elementary,
        String secondary,
        String university
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(this, Description::preschool, "preschool")
                .notNull(this, Description::elementary, "elementary")
                .notNull(this, Description::secondary, "secondary")
                .notNull(this, Description::university, "university");
    }
}
