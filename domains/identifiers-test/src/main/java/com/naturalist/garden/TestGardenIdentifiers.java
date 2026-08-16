package com.naturalist.garden;

import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.zone.ZoneName;

import java.util.UUID;

/**
 * Single source of truth for garden {@code EntityName} / {@code EntityId} constants used in
 * repository and query contract tests. Mirrors the domain object graph: {@code Planting}s are
 * grouped by the bed they are in, which is how a {@code PlantedZone} is assembled. Ids match the
 * fixture JSON exactly.
 * <p>
 * PlantSpecies and cultivar constants name plants-domain entities. Garden records that they were planted;
 * plants owns what they are.
 */
public final class TestGardenIdentifiers {

    private TestGardenIdentifiers() {
    }

    public static final class Plants {

        private Plants() {
        }

        public static final PlantSpeciesName tomato = PlantSpeciesName.of("solanum-lycopersicum");
        public static final PlantSpeciesName basil = PlantSpeciesName.of("ocimum-basilicum");
        public static final PlantSpeciesName eggplant = PlantSpeciesName.of("solanum-melongena");
        public static final PlantSpeciesName radish = PlantSpeciesName.of("raphanus-sativus");

        public static final PlantSpeciesName notFound = PlantSpeciesName.of("unobtainium-vulgaris");
    }

    public static final class Cultivars {

        private Cultivars() {
        }

        public static final CultivarName amishPaste = CultivarName.of("amish-paste");
        public static final CultivarName italianPearNicks = CultivarName.of("italian-pear-nicks");
        public static final CultivarName sanMarzanoF2 = CultivarName.of("san-marzano-f2");
        public static final CultivarName sungoldCherry = CultivarName.of("sungold-cherry");
    }

    /** The two beds Oak Vista both plants and soil-samples. */
    public static final class Zones {

        private Zones() {
        }

        public static final ZoneName box1 = ZoneName.of("box-1");
        public static final ZoneName backyard = ZoneName.of("backyard");

        public static final ZoneName notFound = ZoneName.of("unobtainium-bed");
    }

    public static final class Plantings {

        private Plantings() {
        }

        /** Twelve Amish Paste, back yard south row, April 6 2026. */
        public static final PlantingId amishPasteBackyard =
                PlantingId.of(UUID.fromString("02671301-0000-7000-8000-000003100000"));

        /** The 50-year heirloom line, back yard centre. */
        public static final PlantingId italianPearBackyard =
                PlantingId.of(UUID.fromString("02671301-0001-7000-8000-000003100001"));

        /** The north row, which carried the April 7 TSWV outbreak. */
        public static final PlantingId sanMarzanoBackyard =
                PlantingId.of(UUID.fromString("02671301-0002-7000-8000-000003100002"));

        /** A single plant in Box 1 — the zone-scoped case, no sub-zone. */
        public static final PlantingId sungoldBox1 =
                PlantingId.of(UUID.fromString("02671301-0003-7000-8000-000003100003"));

        /** Still in the ground: the active-planting case. */
        public static final PlantingId genoveseBasilBox1 =
                PlantingId.of(UUID.fromString("02671301-0004-7000-8000-000003100004"));

        /** Shares the back yard south row with the Amish Paste tomatoes. */
        public static final PlantingId blackBeautyBackyard =
                PlantingId.of(UUID.fromString("02671301-0007-7000-8000-000003100007"));

        /** Sown from a mixed packet — the plant is known and the variety never was. */
        public static final PlantingId radishBox1 =
                PlantingId.of(UUID.fromString("02671301-0008-7000-8000-000003100008"));

        public static final PlantingId notFound =
                PlantingId.of(UUID.fromString("02671399-0000-7000-8000-000003900000"));
    }
}
