package com.naturalist.plants.phytochemistry;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.EntityQuery;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentEntityCollections.PhytochemicalConstituentCollection;

/**
 * Namespace query for the phytochemistry sub-context — the single
 * discoverable entry point for reading phytochemical-constituent data.
 *
 * <p>The two cross-entity rollups are the heart of the consumer surface:
 * <ul>
 *   <li>{@link PhytochemicalConstituentEntityQuery#forPlantName(PlantSpeciesName)} —
 *       what compounds does a plant produce?</li>
 *   <li>{@link PhytochemicalConstituentEntityQuery#forCompoundName(CompoundName)} —
 *       what plants produce a given compound? (cross-domain reverse lookup)</li>
 * </ul>
 */
public interface PhytochemicalConstituentQuery {

    PhytochemicalConstituentEntityQuery constituents();

    interface PhytochemicalConstituentEntityQuery
            extends EntityQuery<PhytochemicalConstituentName, PhytochemicalConstituent, PhytochemicalConstituentCollection> {

        PhytochemicalConstituentCollection forPlantName(PlantSpeciesName plantName);

        PhytochemicalConstituentCollection forCompoundName(CompoundName compoundName);
    }
}
