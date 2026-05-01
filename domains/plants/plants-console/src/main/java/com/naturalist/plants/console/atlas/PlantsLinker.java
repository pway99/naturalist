package com.naturalist.plants.console.atlas;

import com.naturalist.atlas.EntityRef;
import com.naturalist.atlas.EntityRefLinker;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.plants.heritage.SeedLineageName;
import com.naturalist.plants.management.PlantProgramName;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentName;
import org.springframework.stereotype.Component;

/**
 * Plants-domain {@link EntityRefLinker}: maps every plants-owned
 * {@code EntityName} type to the URL of its detail page on the plants
 * console. The single place to look when adding a new plants entity or
 * moving an existing one to a new route.
 */
@Component
public class PlantsLinker implements EntityRefLinker {

    @Override
    public String linkFor(EntityRef ref) {
        return switch (ref.name()) {
            case PlantName n                    -> "/plants/" + n.value();
            case CultivarName n                 -> "/plants/cultivars/" + n.value();
            case SeedLineageName n              -> "/plants/lineages/" + n.value();
            case PlantProgramName n             -> "/plants/programs/" + n.value();
            case PhytochemicalConstituentName n -> "/plants/phytochemistry/" + n.value();
            default -> null;
        };
    }
}
