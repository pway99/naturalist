package com.naturalist.insects.lifestage;

import com.naturalist.habitat.HabitatZone;
import com.naturalist.ddd.Enums;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/** Child row for one {@link HabitatZone} of a life stage's habitat profile. Carries the life stage NAME. */
@DboSchema(table = "insect_life_stage_habitat_zone", primaryKey = "life_stage_id,zone",
        foreignKeys = @Fk(columns = "life_stage_id", references = "insect_life_stage(id)"),
        entity = LifeStage.class)
final class InsectLifeStageHabitatZoneDbo implements Dbo {
    String lifeStageName;
    String zone;

    private static final Observer observer = Observer.forClass(InsectLifeStageHabitatZoneDbo.class);

    static InsectLifeStageHabitatZoneDbo from(String lifeStageName, HabitatZone zone) {
        observer.arguments("from", i -> i
                .kebabFormat(lifeStageName, "lifeStageName")
                .notNull(zone, "zone"))
                .throwWhenInvalid();
        InsectLifeStageHabitatZoneDbo d = new InsectLifeStageHabitatZoneDbo();
        d.lifeStageName = lifeStageName;
        d.zone = zone.name();
        return d;
    }

    HabitatZone toZone() {
        observer.state("toZone", s -> s.observable(this, "row")).observe(Level.WARN);
        return Enums.named(HabitatZone.class, zone);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .kebabFormat(lifeStageName, "lifeStageName")
                .notBlank(zone, "zone")
                .maxLength(zone, 24, "zone")
                .enumValue(zone, HabitatZone.class, "zone");
    }

}
