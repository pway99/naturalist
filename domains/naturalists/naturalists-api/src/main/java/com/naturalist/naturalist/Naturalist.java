package com.naturalist.naturalist;

import com.naturalist.ddd.CatalogEntity;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A person who participates in the life of Oak Vista — as visitor, caretaker,
 * student, keeper, or teacher.
 * <p>
 * The name is inspired by Gerald Durrell, who spent his life building institutions
 * where people of every background could connect with the natural world. Every
 * person who enters Oak Vista is, in some sense, a naturalist in the making.
 * <p>
 * The {@link EcologicalStage} field captures where this person currently sits in
 * the Durrell learning progression. The application layer uses it to select the
 * appropriate {@link com.naturalist.fieldnotes.Description} level when presenting
 * catalog knowledge — surfacing wonder for the young visitor, mechanism for the
 * practitioner, full taxonomy for the keeper reviewing nematode application notes.
 * <p>
 * {@code givenName} and {@code familyName} are plain strings for display. The
 * {@link NaturalistName} slug is the stable identity used in all cross-domain
 * references and JSON catalogs.
 */
public record Naturalist(
        NaturalistId id,
        NaturalistName name,
        String givenName,
        @Nullable String familyName,
        NaturalistRole role,
        EcologicalStage stage,
        @Nullable String notes
) implements CatalogEntity<NaturalistId, NaturalistName> {

    @Override
    public Naturalist withId(NaturalistId id) {
        return new Naturalist(id, name, givenName, familyName, role, stage, notes);
    }

    /**
     * Whether this naturalist can lead others in ecological observation.
     * True for TEACHER role only.
     */
    public boolean isTeacher() {
        return role == NaturalistRole.TEACHER;
    }

    /**
     * Whether this naturalist manages the apiary.
     * Keepers must be at PRACTITIONER stage or above for hive responsibilities.
     */
    public boolean isKeeper() {
        return role == NaturalistRole.KEEPER;
    }

    /**
     * Whether this naturalist's current stage is sufficient for apiary management.
     * A keeper at WONDER or CURIOUS stage should not be attributed with
     * independent chemical treatment decisions.
     */
    public boolean isApiaryCompetent() {
        return isKeeper() && (stage == EcologicalStage.PRACTITIONER
                || stage == EcologicalStage.NATURALIST);
    }

    /**
     * Display name — given name and family name combined, or just given name
     * if family name is absent.
     */
    public String displayName() {
        return familyName != null ? givenName + " " + familyName : givenName;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(name, "name")
                .notNull(this, Naturalist::givenName, "givenName")
                .notNull(this, Naturalist::role, "role")
                .notNull(this, Naturalist::stage, "stage");
    }
}
