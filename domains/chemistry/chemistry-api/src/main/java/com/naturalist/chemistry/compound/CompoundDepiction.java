package com.naturalist.chemistry.compound;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityIdentifier;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * The 2D structural depiction inputs for a {@link Compound} — the SMILES string and a
 * domain note describing how the rendered structure relates to the catalogued species
 * (canonical molecule, ionic formula unit, or representative stand-in).
 * <p>
 * {@code CompoundDepiction} is an {@link Entity} with surrogate {@link DepictionId}
 * identity. Rendering inputs have no natural slug — a depiction is a row attached to a
 * compound, not a globally-named thing — so the surrogate-id branch of the identity
 * model applies (ADR-022).
 * <p>
 * {@code compoundName} is the slug of the parent {@link Compound} and the foreign-key
 * reference into the compound aggregate. It is annotated {@link EntityIdentifier} to
 * declare the secondary unique constraint: at most one depiction exists per compound.
 * <p>
 * {@code smiles} is a SMILES (Simplified Molecular-Input Line-Entry System) string —
 * the standard machine-readable encoding of a 2D molecular structure. {@code note} is
 * domain commentary clarifying the depiction's relationship to the compound:
 * <ul>
 *   <li>Discrete organic molecules carry their canonical structure and the note
 *       describes structural distinctions.</li>
 *   <li>Ionic salts encode the formula unit as discrete ions
 *       ({@code [Ca+2].[Cl-].[Cl-]}) and the note flags the ionic depiction.</li>
 *   <li>Class-label catalog entries (e.g. {@code organic-acid-chelate},
 *       {@code potassium-fatty-acids}) carry a representative stand-in molecule and
 *       the note flags this so consumers do not mistake the rendering for the
 *       compound's exact structure.</li>
 * </ul>
 * The depiction is consumed by the chemistry console's CDK-based SVG renderer.
 */
public record CompoundDepiction(
        DepictionId id,
        @EntityIdentifier CompoundName compoundName,
        String smiles,
        String note
) implements Entity<DepictionId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(compoundName, "compoundName")
                .notBlank(smiles, "smiles")
                .notBlank(note, "note");
    }
}
