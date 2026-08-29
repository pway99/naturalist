package com.naturalist.soil;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;

import java.util.function.Consumer;

/**
 * Flat persistence view of {@link SoilProfileInfo} — a slug-identified {@code NamedEntity} whose
 * only fields are the two spatial soft-references into the zone domain. {@code zoneName} is a plain
 * slug column (always present); {@code subZoneName} is a plain nullable slug column. Neither carries
 * a foreign key — the zone skeleton is a peer domain, resolved by name in the application layer.
 * {@code id} is a DB-generated {@code BIGINT IDENTITY}, the durable FK anchor, and never surfaces in
 * the domain. Fields are camelCase; MyBatis translates the snake_case columns across on read.
 */
@DboSchema(table = "soil_profile_info", primaryKey = "id", unique = {"name"}, entity = SoilProfileInfo.class)
final class SoilProfileInfoDbo implements Dbo {
    Long id;            // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String zoneName;
    String subZoneName; // nullable

    static SoilProfileInfoDbo from(SoilProfileInfo p) {
        SoilProfileInfoDbo d = new SoilProfileInfoDbo();
        d.name = p.name().value();
        d.zoneName = p.zoneName().value();
        d.subZoneName = p.subZoneName() == null ? null : p.subZoneName().value();
        Observer.forClass(SoilProfileInfoDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    SoilProfileInfo toEntity() {
        return new SoilProfileInfo(
                SoilProfileName.of(name),
                ZoneName.of(zoneName),
                subZoneName == null ? null : SubZoneName.of(subZoneName));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")
                .notNull(zoneName, "zoneName").kebabFormat(zoneName, "zoneName").maxLength(zoneName, 64, "zoneName")
                .maxLength(subZoneName, 64, "subZoneName");
    }
}
