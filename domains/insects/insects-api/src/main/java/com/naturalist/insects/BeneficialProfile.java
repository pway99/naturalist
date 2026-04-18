package com.naturalist.insects;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Structured pest-management and ecological significance data for species whose
 * {@link InsectSpecies#beneficial()} flag is {@code true}.
 * <p>
 * This is a nullable field on {@link InsectSpecies} — it is only populated for
 * documented beneficial species. Future catalog entries for pest species or
 * ecologically neutral species will carry {@code null} here.
 * <p>
 * {@code significance} is the primary field — a concise statement of why this
 * species matters at Oak Vista, oriented toward the naturalist rather than the
 * pest manager.
 * <p>
 * {@code pestManagementValue} describes the IPM role: whether the species is
 * commercially available for release, whether it arrived naturally, and the
 * practical pest suppression contribution at the garden scale.
 * <p>
 * {@code ipmNotes} captures management guidance specific to this species —
 * conditions for purchase and release, habitat setup required to support
 * establishment, or reasons why commercial release is or is not appropriate
 * (e.g. purchased overwintered ladybugs disperse immediately and provide no
 * pest control value; captive on-site rearing is the correct strategy).
 */
public record BeneficialProfile(
        String significance,
        @Nullable String pestManagementValue,
        @Nullable String ipmNotes
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.notNull(this, BeneficialProfile::significance, "significance");
    }
}
