package com.naturalist.naturalist;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.util.function.Consumer;

@DboSchema(table = "naturalist", primaryKey = "id", unique = {"name"}, entity = Naturalist.class)
final class NaturalistDbo implements Dbo {
    Long id;              // null before insert; DB identity fills it
    String name;
    String givenName;
    String familyName;   // nullable
    String role;
    String stage;
    String notes;         // nullable

    static NaturalistDbo from(Naturalist n) {
        NaturalistDbo d = new NaturalistDbo();
        d.name = n.name().value();
        d.givenName = n.givenName();
        d.familyName = n.familyName();
        d.role = n.role().name();
        d.stage = n.stage().name();
        d.notes = n.notes();
        Observer.forClass(NaturalistDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    Naturalist toEntity() {
        return new Naturalist(
                NaturalistName.of(name),
                givenName,
                familyName,
                NaturalistRole.valueOf(role),
                EcologicalStage.valueOf(stage),
                notes);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")
                .notNull(givenName, "givenName").maxLength(givenName, 100, "givenName")
                .maxLength(familyName, 100, "familyName")
                .notNull(role, "role").maxLength(role, 32, "role")
                .notNull(stage, "stage").maxLength(stage, 32, "stage");
    }
}
