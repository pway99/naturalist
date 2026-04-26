package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.compound.role.FunctionalRole;
import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Namespace for the compound sub-context's {@link BehavioralCollection} return types.
 *
 * <p>Nested collections scope to a single entity each:
 * <ul>
 *   <li>{@link CompoundCollection} — multi-result return type for {@link Compound}.</li>
 *   <li>{@link DepictionCollection} — multi-result return type for {@link CompoundDepiction}.</li>
 * </ul>
 *
 * <p>{@code CompoundEntityCollections} is a pure container — it holds no behavior of its own,
 * mirroring the {@link CompoundQuery} pattern: one file per namespace, nested types for
 * everything inside (ADR-020).
 */
public interface CompoundEntityCollections {

    final class CompoundCollection extends BehavioralCollection<Compound> {

        CompoundCollection(Collection<Compound> compounds) {
            super(compounds);
        }

        public static CompoundCollection of(Collection<Compound> compounds) {
            return new CompoundCollection(compounds);
        }

        public static CompoundCollection empty() {
            return new CompoundCollection(List.of());
        }

        public CompoundCollection withChemicalNature(ChemicalNature nature) {
            return new CompoundCollection(
                    stream()
                            .filter(c -> c.compoundInfo().chemicalNature() == nature)
                            .toList()
            );
        }

        public CompoundCollection withFunctionalRole(FunctionalRole role) {
            return new CompoundCollection(
                    stream()
                            .filter(c -> c.playsRole(role))
                            .toList()
            );
        }
    }

    final class DepictionCollection extends BehavioralCollection<CompoundDepiction> {

        DepictionCollection(Collection<CompoundDepiction> depictions) {
            super(depictions);
        }

        public static DepictionCollection of(Collection<CompoundDepiction> depictions) {
            return new DepictionCollection(depictions);
        }

        public static DepictionCollection empty() {
            return new DepictionCollection(List.of());
        }

        /**
         * Look up the depiction for a given compound within this collection. The
         * {@code compoundName} field is unique on {@link CompoundDepiction} (one
         * depiction per compound), so the result is at most one entity.
         */
        public Optional<CompoundDepiction> getByCompoundName(CompoundName compoundName) {
            return stream()
                    .filter(depiction -> depiction.compoundName().equals(compoundName))
                    .findFirst();
        }
    }
}
