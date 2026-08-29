package com.naturalist.insects.lifestage;

import com.naturalist.habitat.HabitatZone;
import com.naturalist.observability.Constraints;
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

    static InsectLifeStageHabitatZoneDbo from(String lifeStageName, HabitatZone zone) {
        InsectLifeStageHabitatZoneDbo d = new InsectLifeStageHabitatZoneDbo();
        d.lifeStageName = lifeStageName;
        d.zone = zone.name();
        Observer.forClass(InsectLifeStageHabitatZoneDbo.class)
                .arguments("from", i -> i.observable(d, "dbo")).throwWhenInvalid();
        return d;
    }

    HabitatZone toZone() { return HabitatZone.valueOf(zone); }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c.notNull(lifeStageName, "lifeStageName").kebabFormat(lifeStageName, "lifeStageName")
                .notBlank(zone, "zone").maxLength(zone, 24, "zone");
    }
}
