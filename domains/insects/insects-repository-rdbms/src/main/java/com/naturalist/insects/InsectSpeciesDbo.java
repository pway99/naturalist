package com.naturalist.insects;

import com.naturalist.clades.Clade;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.habitat.HabitatProfile;
import com.naturalist.habitat.HabitatZone;
import com.naturalist.habitat.LightRegime;
import com.naturalist.habitat.MoistureRegime;
import com.naturalist.habitat.VerticalLayer;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;
import com.naturalist.taxonomy.TaxonomicSpecies;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Parent row of the {@link InsectSpecies} aggregate — the bottom of the insect Linnaean chain. The
 * upward reference to the parent {@link InsectGenus} is stored as {@code genus_id} (FK-enforced); the
 * DBO carries the genus's {@code name} in {@link #genusName} for the mapper's JOIN projection (read)
 * and nested-select (write).
 *
 * <p>The seven owned {@code @Nullable} value objects flatten onto null-group columns here — each
 * group is present exactly when its presence column (or, for the two child-backed groups, its child
 * rows) is non-null, so a single required field per group serves as its presence flag:
 * <ul>
 *   <li>{@code ChemicalDefense} — present iff {@code chem_mechanism} non-null; {@code protectedStages}
 *       in {@link InsectSpeciesProtectedStageDbo}.</li>
 *   <li>{@code Voltinism} — present iff {@code voltinism_pattern} non-null.</li>
 *   <li>{@code HabitatProfile} — present iff a {@link InsectSpeciesHabitatZoneDbo} row exists (zones is
 *       required non-empty); layers in {@link InsectSpeciesHabitatLayerDbo}.</li>
 *   <li>{@code HabitatRequirements} — present iff any {@code req_*} column non-null.</li>
 *   <li>{@code GardenConnections} — present iff a {@link InsectSpeciesSupportingPlantDbo} row exists
 *       OR either garden text column non-null; {@code supportingPlants} is an ordered child list.</li>
 *   <li>{@code BeneficialProfile} — present iff {@code beneficial_significance} non-null.</li>
 *   <li>{@code EcologicalSignificance} — present iff any {@code eco_*} column non-null.</li>
 * </ul>
 *
 * <p>Because the aggregate spans several tables, this DBO carries no bare {@code toEntity()}: the
 * adapter loads the five child collections and passes them to
 * {@link #toEntity(Set, Set, Set, Set, List)}.
 */
@DboSchema(table = "insect_species", primaryKey = "id", unique = {"name"},
           foreignKeys = @Fk(columns = "genus_id", references = "insect_genus(id)"),
           entity = InsectSpecies.class)
final class InsectSpeciesDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String genusName;     // JOIN projection (read) / nested-select key (write); stored as genus_id
    String epithet;
    String placedIn;      // nullable clade slug (no FK)
    String sightingNotes; // nullable

    // ── ChemicalDefense (null group: present iff chemMechanism non-null) ──────
    String chemMechanism;
    String chemSourceCompounds;
    String chemAposematicSignal;

    // ── Voltinism (null group: present iff voltinismPattern non-null) ─────────
    String voltinismPattern;
    String voltinismNotes;

    // ── HabitatProfile (null group: present iff a habitat_zone child row exists) ──
    String habitatMoisture;
    String habitatLight;

    // ── HabitatRequirements (null group: present iff any req_* non-null) ──────
    String reqNectarSources;
    String reqShelter;
    String reqPreyAvailability;
    String reqLighting;
    String reqElevationRange;

    // ── GardenConnections (null group: present iff a supporting_plant child row exists OR either text non-null) ──
    String gardenRelationshipToOtherBeneficials;
    String gardenNaturalEnemies;

    // ── BeneficialProfile (null group: present iff beneficialSignificance non-null) ──
    String beneficialSignificance;
    String beneficialPestManagementValue;
    String beneficialIpmNotes;

    // ── EcologicalSignificance (null group: present iff any eco_* non-null) ───
    String ecoIndicatorValue;
    String ecoFoodWebPosition;
    String ecoRegionalContext;

    String descriptionPreschool;
    String descriptionElementary;
    String descriptionSecondary;
    String descriptionUniversity;

    static InsectSpeciesDbo from(InsectSpecies s) {
        InsectSpeciesDbo d = new InsectSpeciesDbo();
        d.name = s.name().value();
        d.genusName = s.genusName().value();
        d.epithet = s.epithet().value();
        d.placedIn = s.placedIn() == null ? null : s.placedIn().slug();
        d.sightingNotes = s.sightingNotes();

        Description desc = s.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();

        InsectSpecies.ChemicalDefense chem = s.chemicalDefense();
        if (chem != null) {
            d.chemMechanism = chem.mechanism();
            d.chemSourceCompounds = chem.sourceCompounds();
            d.chemAposematicSignal = chem.aposematicSignal();
        }

        InsectSpecies.Voltinism voltinism = s.voltinism();
        if (voltinism != null) {
            d.voltinismPattern = voltinism.pattern().name();
            d.voltinismNotes = voltinism.notes();
        }

        HabitatProfile habitat = s.habitatProfile();
        if (habitat != null) {
            d.habitatMoisture = habitat.moisture() == null ? null : habitat.moisture().name();
            d.habitatLight = habitat.light() == null ? null : habitat.light().name();
        }

        InsectSpecies.HabitatRequirements req = s.habitatRequirements();
        if (req != null) {
            d.reqNectarSources = req.nectarSources();
            d.reqShelter = req.shelter();
            d.reqPreyAvailability = req.preyAvailability();
            d.reqLighting = req.lighting();
            d.reqElevationRange = req.elevationRange();
        }

        InsectSpecies.GardenConnections garden = s.gardenConnections();
        if (garden != null) {
            d.gardenRelationshipToOtherBeneficials = garden.relationshipToOtherBeneficials();
            d.gardenNaturalEnemies = garden.naturalEnemies();
        }

        InsectSpecies.BeneficialProfile beneficial = s.beneficialProfile();
        if (beneficial != null) {
            d.beneficialSignificance = beneficial.significance();
            d.beneficialPestManagementValue = beneficial.pestManagementValue();
            d.beneficialIpmNotes = beneficial.ipmNotes();
        }

        InsectSpecies.EcologicalSignificance eco = s.ecologicalSignificance();
        if (eco != null) {
            d.ecoIndicatorValue = eco.indicatorValue();
            d.ecoFoodWebPosition = eco.foodWebPosition();
            d.ecoRegionalContext = eco.regionalContext();
        }

        Observer.forClass(InsectSpeciesDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    /**
     * Reassembles the aggregate from this parent row plus its child-table members. The adapter
     * supplies the {@code commonNames}, {@code protectedStages}, {@code zones}, {@code layers}, and
     * ordered {@code supportingPlants} it loaded from the child tables. Each nullable value object is
     * reconstructed only when its null-group presence condition holds, so an all-null species
     * round-trips as absent (null) on every owned VO.
     */
    InsectSpecies toEntity(Set<CommonName> commonNames,
                           Set<LifeStageKind> protectedStages,
                           Set<HabitatZone> zones,
                           Set<VerticalLayer> layers,
                           List<String> supportingPlants) {
        Description description = new Description(
                descriptionPreschool,
                descriptionElementary,
                descriptionSecondary,
                descriptionUniversity);

        InsectSpecies.ChemicalDefense chemicalDefense = chemMechanism == null ? null
                : new InsectSpecies.ChemicalDefense(
                        chemMechanism, chemSourceCompounds, chemAposematicSignal, protectedStages);

        InsectSpecies.Voltinism voltinism = voltinismPattern == null ? null
                : new InsectSpecies.Voltinism(
                        InsectSpecies.Voltinism.VoltinismPattern.valueOf(voltinismPattern), voltinismNotes);

        HabitatProfile habitatProfile = zones.isEmpty() ? null
                : new HabitatProfile(
                        zones,
                        habitatMoisture == null ? null : MoistureRegime.valueOf(habitatMoisture),
                        habitatLight == null ? null : LightRegime.valueOf(habitatLight),
                        layers.isEmpty() ? null : layers);

        boolean anyRequirement = reqNectarSources != null || reqShelter != null
                || reqPreyAvailability != null || reqLighting != null || reqElevationRange != null;
        InsectSpecies.HabitatRequirements habitatRequirements = anyRequirement
                ? new InsectSpecies.HabitatRequirements(
                        reqNectarSources, reqShelter, reqPreyAvailability, reqLighting, reqElevationRange)
                : null;

        boolean gardenPresent = !supportingPlants.isEmpty()
                || gardenRelationshipToOtherBeneficials != null || gardenNaturalEnemies != null;
        InsectSpecies.GardenConnections gardenConnections = gardenPresent
                ? new InsectSpecies.GardenConnections(
                        supportingPlants, gardenRelationshipToOtherBeneficials, gardenNaturalEnemies)
                : null;

        InsectSpecies.BeneficialProfile beneficialProfile = beneficialSignificance == null ? null
                : new InsectSpecies.BeneficialProfile(
                        beneficialSignificance, beneficialPestManagementValue, beneficialIpmNotes);

        boolean anyEcological = ecoIndicatorValue != null || ecoFoodWebPosition != null
                || ecoRegionalContext != null;
        InsectSpecies.EcologicalSignificance ecologicalSignificance = anyEcological
                ? new InsectSpecies.EcologicalSignificance(
                        ecoIndicatorValue, ecoFoodWebPosition, ecoRegionalContext)
                : null;

        return new InsectSpecies(
                InsectSpeciesName.of(name),
                InsectGenusName.of(genusName),
                TaxonomicSpecies.of(epithet),
                description,
                commonNames,
                sightingNotes,
                placedIn == null ? null : Clade.of(placedIn),
                chemicalDefense,
                voltinism,
                habitatProfile,
                habitatRequirements,
                gardenConnections,
                beneficialProfile,
                ecologicalSignificance);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")
                .notNull(genusName, "genusName").kebabFormat(genusName, "genusName")
                .notBlank(epithet, "epithet").maxLength(epithet, 128, "epithet")
                .maxLength(placedIn, 64, "placedIn")
                .maxLength(voltinismPattern, 24, "voltinismPattern")
                .maxLength(habitatMoisture, 24, "habitatMoisture")
                .maxLength(habitatLight, 24, "habitatLight")
                .notBlank(descriptionPreschool, "descriptionPreschool")
                .notBlank(descriptionElementary, "descriptionElementary")
                .notBlank(descriptionSecondary, "descriptionSecondary")
                .notBlank(descriptionUniversity, "descriptionUniversity");
    }
}
