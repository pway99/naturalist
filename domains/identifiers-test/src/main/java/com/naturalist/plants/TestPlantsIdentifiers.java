package com.naturalist.plants;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.plants.heritage.SeedLineageName;
import com.naturalist.plants.management.PlantProgramName;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentName;
import java.util.UUID;

/**
 * Hardcoded EntityName constants for deterministic plants repository test authoring.
 * <p>
 * Class structure mirrors the domain object graph: each plant gets its own static class
 * holding its {@code name} and any child entity collections nested beneath it (programs,
 * future observations, etc.). Every entity type defines at least two known names (for
 * single-entity and set-based lookup tests) and a {@code NotFound} inner class containing
 * fictitious names guaranteed absent from any JSON catalog.
 * <p>
 * Usage in contract tests:
 * <pre>
 *     repository().getByName(TestPlantsIdentifiers.Plants.CaliforniaPipevine.name)
 *     repository().getByName(TestPlantsIdentifiers.Plants.CaliforniaPipevine.Programs.PesticideExclusion)
 *     repository().getByName(TestPlantsIdentifiers.Plants.NotFound.name)
 * </pre>
 */
public class TestPlantsIdentifiers {


    private TestPlantsIdentifiers() {
    }

    /**
     * Plant field-observation ids (surrogate UUIDv7). Fixed so the JSON fixture and the
     * contract tests agree. Patrick and Delia both observe solanum-lycopersicum, which lets
     * the naturalist-scoped query tests prove exclusion.
     */
    public static class FieldObservations {

        private FieldObservations() {
        }

        public static class NotFound {
            public static final PlantObservationId id =
                    PlantObservationId.of(UUID.fromString("026aa000-0000-7000-8000-0000000000ff"));
        }

        public static final PlantObservationId PatrickTomato =
                PlantObservationId.of(UUID.fromString("026aa000-0000-7000-8000-000000000001"));
        public static final PlantObservationId DeliaTomato =
                PlantObservationId.of(UUID.fromString("026aa000-0000-7000-8000-000000000002"));
        public static final PlantObservationId PatrickTrifolium =
                PlantObservationId.of(UUID.fromString("026aa000-0000-7000-8000-000000000003"));
        public static final PlantObservationId PatrickLamiaceae =
                PlantObservationId.of(UUID.fromString("026aa000-0000-7000-8000-000000000004"));
    }

    /**
     * Plant feature ids (surrogate UUIDv7). Fixed so the JSON fixture and the
     * contract tests agree.
     */
    public static class PlantFeatures {

        private PlantFeatures() {
        }

        public static class NotFound {
            public static final PlantFeatureId id =
                    PlantFeatureId.of(UUID.fromString("026cc000-0000-7000-8000-0000000000ff"));
        }

        public static final PlantFeatureId RayFlorets =
                PlantFeatureId.of(UUID.fromString("026cc000-0000-7000-8000-000000000001"));
        public static final PlantFeatureId OppositeLeaves =
                PlantFeatureId.of(UUID.fromString("026cc000-0000-7000-8000-000000000002"));
    }

    /**
     * Plant feature-assignment ids (surrogate UUIDv7) — the feature↔rank link.
     * Fixed so the JSON fixture and the contract tests agree.
     */
    public static class PlantFeatureAssignments {

        private PlantFeatureAssignments() {
        }

        public static class NotFound {
            public static final PlantFeatureAssignmentId id =
                    PlantFeatureAssignmentId.of(UUID.fromString("026dd000-0000-7000-8000-0000000000ff"));
        }

        public static final PlantFeatureAssignmentId RayFloretsAsteraceae =
                PlantFeatureAssignmentId.of(UUID.fromString("026dd000-0000-7000-8000-000000000001"));
        public static final PlantFeatureAssignmentId OppositeLeavesLamiaceae =
                PlantFeatureAssignmentId.of(UUID.fromString("026dd000-0000-7000-8000-000000000002"));
    }

    public static class PlantOrders {

        private PlantOrders() {
        }

        /** Fictitious identifier for the {@link com.naturalist.plants.PlantOrder} scope. */
        public static class NotFound {
            public static final PlantOrderName name = PlantOrderName.of("unobtainium-ales");
        }

        public static class Lamiales {
            public static final PlantOrderName name = PlantOrderName.of("lamiales");
        }

        public static class Piperales {
            public static final PlantOrderName name = PlantOrderName.of("piperales");
        }

        public static class Rosales {
            public static final PlantOrderName name = PlantOrderName.of("rosales");
        }

        /** The only order with more than one catalogued family — Passifloraceae and Violaceae. */
        public static class Malpighiales {
            public static final PlantOrderName name = PlantOrderName.of("malpighiales");
        }

        /** Sunflower order — authored with the front-meadow Helianthus for the S2 image stack. */
        public static class Asterales {
            public static final PlantOrderName name = PlantOrderName.of("asterales");
        }
    }

    public static class PlantFamilies {

        private PlantFamilies() {
        }

        /**
         * Fictitious identifier for the {@link com.naturalist.plants.PlantFamily}
         * scope — guaranteed absent from any catalog.
         */
        public static class NotFound {
            public static final PlantFamilyName name = PlantFamilyName.of("unobtainium-aceae");
        }

        public static class Apiaceae {
            public static final PlantFamilyName name = PlantFamilyName.of("apiaceae");
        }

        /** Daisy/sunflower family — authored for the S2 image stack (front-meadow Helianthus). */
        public static class Asteraceae {
            public static final PlantFamilyName name = PlantFamilyName.of("asteraceae");
        }

        public static class Aristolochiaceae {
            public static final PlantFamilyName name = PlantFamilyName.of("aristolochiaceae");
        }

        public static class Boraginaceae {
            public static final PlantFamilyName name = PlantFamilyName.of("boraginaceae");
        }

        public static class Brassicaceae {
            public static final PlantFamilyName name = PlantFamilyName.of("brassicaceae");
        }

        public static class Caryophyllaceae {
            public static final PlantFamilyName name = PlantFamilyName.of("caryophyllaceae");
        }

        public static class Fabaceae {
            public static final PlantFamilyName name = PlantFamilyName.of("fabaceae");
        }

        public static class Geraniaceae {
            public static final PlantFamilyName name = PlantFamilyName.of("geraniaceae");
        }

        public static class Lamiaceae {
            public static final PlantFamilyName name = PlantFamilyName.of("lamiaceae");
        }

        public static class Passifloraceae {
            public static final PlantFamilyName name = PlantFamilyName.of("passifloraceae");
        }

        public static class Poaceae {
            public static final PlantFamilyName name = PlantFamilyName.of("poaceae");
        }

        public static class Rosaceae {
            public static final PlantFamilyName name = PlantFamilyName.of("rosaceae");
        }

        public static class Rutaceae {
            public static final PlantFamilyName name = PlantFamilyName.of("rutaceae");
        }

        public static class Solanaceae {
            public static final PlantFamilyName name = PlantFamilyName.of("solanaceae");
        }

        public static class Violaceae {
            public static final PlantFamilyName name = PlantFamilyName.of("violaceae");
        }
    }

    public static class PlantGenera {

        private PlantGenera() {
        }

        /**
         * Fictitious identifier for the {@link com.naturalist.plants.PlantGenus}
         * scope — guaranteed absent from any catalog.
         */
        public static class NotFound {
            public static final PlantGenusName name = PlantGenusName.of("unobtainium-genus");
        }

        /**
         * Demoted from a species-rank {@code creeping-thyme} row in the 2026-08-16 rank
         * audit — the record never named a species. Its thymol constituent came with it.
         */
        public static class Thymus {
            public static final PlantGenusName name = PlantGenusName.of("thymus");

            public static class Constituents {
                private Constituents() {
                }

                public static final PhytochemicalConstituentName Thymol =
                        PhytochemicalConstituentName.of(name, CompoundName.of("thymol"));
            }
        }

        public static class Passiflora {
            public static final PlantGenusName name = PlantGenusName.of("passiflora");
        }

        public static class Dianthus {
            public static final PlantGenusName name = PlantGenusName.of("dianthus");
        }

        public static class Salvia {
            public static final PlantGenusName name = PlantGenusName.of("salvia");
        }

        public static class Citrus {
            public static final PlantGenusName name = PlantGenusName.of("citrus");
        }

        /** Carries two catalogued species (crimson and white clover) — the genus→species rollup test case. */
        public static class Trifolium {
            public static final PlantGenusName name = PlantGenusName.of("trifolium");
        }

        /**
         * Sunflower genus — the front-meadow sunflower is catalogued here at genus rank
         * (species unconfirmed). Carries the S2 sunflower image.
         */
        public static class Helianthus {
            public static final PlantGenusName name = PlantGenusName.of("helianthus");
        }

        /** Tomato genus — the back-garden tomato-patch image attaches here (not a single species). */
        public static class Solanum {
            public static final PlantGenusName name = PlantGenusName.of("solanum");
        }
    }

    public static class Plants {

        private Plants() {
        }

        /**
         * Fictitious identifiers for all entity types within this scope — guaranteed absent from any catalog.
         */
        public static class NotFound {
            public static final PlantSpeciesName name = PlantSpeciesName.of("unobtainium-vine");
            public static final PlantProgramName programName =
                    PlantProgramName.of("unobtainium-program");
            public static final PhytochemicalConstituentName constituentName =
                    PhytochemicalConstituentName.of("unobtainium-vine-unobtainium-acid");
            public static final CultivarName cultivarName =
                    CultivarName.of("unobtainium-cultivar");
            public static final SeedLineageName seedLineageName =
                    SeedLineageName.of("unobtainium-lineage");
            public static final PlantImageId imageId =
                    PlantImageId.of(UUID.fromString("026bb000-0000-7000-8000-0000000000ff"));
        }

        public static class CaliforniaPipevine {
            public static final PlantSpeciesName name = PlantSpeciesName.of("aristolochia-californica");

            /**
             * Two photographs attached at the species rank — the known pair the image
             * repository/query contract tests read. Both were captured under patrick's
             * pipevine field observation (a single trellis-wall sighting, two frames), so
             * they exercise the {@code observationId} link.
             */
            public static class Images {
                private Images() {
                }

                public static final PlantImageId Wide5905 =
                        PlantImageId.of(UUID.fromString("026bb000-0000-7000-8000-000000000001"));
                public static final PlantImageId WideC072 =
                        PlantImageId.of(UUID.fromString("026bb000-0000-7000-8000-000000000002"));
            }

            public static class Programs {
                private Programs() {
                }

                public static final PlantProgramName PesticideExclusion =
                        PlantProgramName.of("pipevine-pesticide-exclusion");
                public static final PlantProgramName LarvalMonitoring =
                        PlantProgramName.of("pipevine-larval-monitoring");
            }

            public static class Constituents {
                private Constituents() {
                }

                public static final PhytochemicalConstituentName AristolochicAcidI =
                        PhytochemicalConstituentName.of(name, CompoundName.of("aristolochic-acid-i"));
                public static final PhytochemicalConstituentName AristolochicAcidII =
                        PhytochemicalConstituentName.of(name, CompoundName.of("aristolochic-acid-ii"));
            }
        }

        public static class Borage {
            public static final PlantSpeciesName name = PlantSpeciesName.of("borago-officinalis");

            public static class Programs {
                private Programs() {
                }

                public static final PlantProgramName VolunteerThinning =
                        PlantProgramName.of("borage-volunteer-thinning");
            }
        }


        public static class Tomato {
            public static final PlantSpeciesName name = PlantSpeciesName.of("solanum-lycopersicum");

            public static class Cultivars {
                private Cultivars() {
                }

                /**
                 * Cultivar with seed lineages — promoted to a nested class per the convention.
                 */
                public static class AmishPaste {
                    public static final CultivarName name = CultivarName.of("amish-paste");

                    public static class Lineages {
                        private Lineages() {
                        }

                        public static final SeedLineageName BakerCreek =
                                SeedLineageName.of("amish-paste-baker-creek");
                    }
                }

                /**
                 * Cultivar with seed lineages — promoted to a nested class per the convention.
                 */
                public static class ItalianPearNicks {
                    public static final CultivarName name = CultivarName.of("italian-pear-nicks");

                    public static class Lineages {
                        private Lineages() {
                        }

                        public static final SeedLineageName Original =
                                SeedLineageName.of("italian-pear-nicks");
                    }
                }

                public static final CultivarName SungoldCherry =
                        CultivarName.of("sungold-cherry");
                public static final CultivarName SanMarzanoF2 =
                        CultivarName.of("san-marzano-f2");
            }
        }
    }
}