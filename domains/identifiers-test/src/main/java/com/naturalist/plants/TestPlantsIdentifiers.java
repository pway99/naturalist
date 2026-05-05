package com.naturalist.plants;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.plants.heritage.SeedLineageName;
import com.naturalist.plants.management.PlantProgramName;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentName;

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

    public static class Plants {

        private Plants() {
        }

        /**
         * Fictitious identifiers for all entity types within this scope — guaranteed absent from any catalog.
         */
        public static class NotFound {
            public static final PlantName name = PlantName.of("unobtainium-vine");
            public static final PlantProgramName programName =
                    PlantProgramName.of("unobtainium-program");
            public static final PhytochemicalConstituentName constituentName =
                    PhytochemicalConstituentName.of("unobtainium-vine-unobtainium-acid");
            public static final CultivarName cultivarName =
                    CultivarName.of("unobtainium-cultivar");
            public static final SeedLineageName seedLineageName =
                    SeedLineageName.of("unobtainium-lineage");
        }

        public static class CaliforniaPipevine {
            public static final PlantName name = PlantName.of("aristolochia-californica");

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
            public static final PlantName name = PlantName.of("borago-officinalis");

            public static class Programs {
                private Programs() {
                }

                public static final PlantProgramName VolunteerThinning =
                        PlantProgramName.of("borage-volunteer-thinning");
            }
        }

        public static class CreepingThyme {
            public static final PlantName name = PlantName.of("creeping-thyme");

            public static class Constituents {
                private Constituents() {
                }

                public static final PhytochemicalConstituentName Thymol =
                        PhytochemicalConstituentName.of(name, CompoundName.of("thymol"));
            }
        }

        public static class Tomato {
            public static final PlantName name = PlantName.of("solanum-lycopersicum");

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
