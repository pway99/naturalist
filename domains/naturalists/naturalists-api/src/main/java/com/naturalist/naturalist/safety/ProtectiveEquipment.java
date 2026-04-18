package com.naturalist.naturalist.safety;

import com.naturalist.ddd.CatalogEntity;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * A piece of personal protective equipment (PPE) worn by a {@link com.naturalist.naturalist.Naturalist}
 * when handling hazardous compounds or performing management tasks.
 * <p>
 * PPE belongs to the naturalist domain because it is fundamentally about the
 * person — what they wear, what protects them, what is required before they
 * may handle a particular compound. Compounds reference PPE requirements
 * indirectly via {@code SafetyProfile.requiresProtectiveEquipment} (a boolean flag);
 * the specific equipment catalogue lives here and is associated with the person,
 * not with the compound.
 * <p>
 * Examples from the Oak Vista catalog:
 * <ul>
 *   <li>{@code chemical-resistant-gloves} — required for oxalic acid and formic acid handling</li>
 *   <li>{@code eye-protection} — required for any fumigant or acid application</li>
 *   <li>{@code n95-respirator-for-vaporization-method} — required for oxalic acid vaporisation</li>
 *   <li>{@code respirator-recommended} — advisory for moderate-hazard compounds</li>
 *   <li>{@code gloves} — general-purpose for neem oil, insecticidal soap</li>
 * </ul>
 */
public record ProtectiveEquipment(
        ProtectiveEquipmentId id,
        ProtectiveEquipmentName name
) implements CatalogEntity<ProtectiveEquipmentId, ProtectiveEquipmentName> {

    @Override
    public ProtectiveEquipment withId(ProtectiveEquipmentId id) {
        return new ProtectiveEquipment(id, name);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(name, "name");
    }
}
