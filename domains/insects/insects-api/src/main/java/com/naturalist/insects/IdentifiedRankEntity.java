package com.naturalist.insects;

/**
 * The rank entity that was identified from a photograph — a sealed sum type
 * carrying whichever Linnaean rank the vision service and authority validation
 * confirmed. The transaction uses pattern matching to dispatch to the
 * appropriate command for persistence.
 *
 * <p>Each permit wraps its rank entity and exposes the polymorphic
 * {@link InsectRankName} for FK validation in the {@link CatalogIdentification}
 * aggregate's invariants.
 */
public sealed interface IdentifiedRankEntity {

    /** The polymorphic rank name for FK validation. */
    InsectRankName rankName();

    record Species(InsectSpeciesInfo species) implements IdentifiedRankEntity {
        @Override
        public InsectRankName rankName() {
            return species == null ? null : species.name();
        }
    }

    record Genus(InsectGenusInfo genus) implements IdentifiedRankEntity {
        @Override
        public InsectRankName rankName() {
            return genus == null ? null : genus.name();
        }
    }

    record Family(InsectFamilyInfo family) implements IdentifiedRankEntity {
        @Override
        public InsectRankName rankName() {
            return family == null ? null : family.name();
        }
    }

    record Order(InsectOrderInfo order) implements IdentifiedRankEntity {
        @Override
        public InsectRankName rankName() {
            return order == null ? null : order.name();
        }
    }
}
