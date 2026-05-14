package com.naturalist.insects;

import java.util.UUID;

/**
 * Hardcoded EntityName constants for deterministic insects repository test authoring.
 * <p>
 * Every entity type defines at least two known names (to enable single-entity and
 * set-based lookup tests) and a {@code NotFound} inner class containing a fictitious name
 * guaranteed never to appear in any JSON catalog (for empty-result assertions).
 * <p>
 * Usage in contract tests:
 * <pre>
 *     repository().getByName(TestInsectsIdentifiers.InsectSpecies.TachinidFly.name)
 *     repository().getByName(TestInsectsIdentifiers.InsectSpecies.NotFound.name)
 * </pre>
 */
public class TestInsectsIdentifiers {

    private TestInsectsIdentifiers() {
    }

    public static class InsectFamily {

        private InsectFamily() {
        }

        /**
         * Fictitious identifier for the {@link com.naturalist.insects.InsectFamily}
         * scope — guaranteed absent from any catalog.
         */
        public static class NotFound {
            public static final InsectFamilyName name = InsectFamilyName.of("unobtainium-flyidae");
        }

        public static class Tachinidae {
            public static final InsectFamilyName name = InsectFamilyName.of("tachinidae");
        }

        public static class Braconidae {
            public static final InsectFamilyName name = InsectFamilyName.of("braconidae");
        }

        public static class Syrphidae {
            public static final InsectFamilyName name = InsectFamilyName.of("syrphidae");
        }

        public static class Carabidae {
            public static final InsectFamilyName name = InsectFamilyName.of("carabidae");
        }

        public static class Tipulidae {
            public static final InsectFamilyName name = InsectFamilyName.of("tipulidae");
        }

        public static class Hesperiidae {
            public static final InsectFamilyName name = InsectFamilyName.of("hesperiidae");
        }

        public static class Halictidae {
            public static final InsectFamilyName name = InsectFamilyName.of("halictidae");
        }

        public static class Andrenidae {
            public static final InsectFamilyName name = InsectFamilyName.of("andrenidae");
        }

        public static class Chrysopidae {
            public static final InsectFamilyName name = InsectFamilyName.of("chrysopidae");
        }

        public static class Cicadellidae {
            public static final InsectFamilyName name = InsectFamilyName.of("cicadellidae");
        }

        public static class Papilionidae {
            public static final InsectFamilyName name = InsectFamilyName.of("papilionidae");
        }
    }

    public static class InsectGenus {

        private InsectGenus() {
        }

        /**
         * Fictitious identifier for the {@link com.naturalist.insects.InsectGenus}
         * scope — guaranteed absent from any catalog.
         */
        public static class NotFound {
            public static final InsectGenusName name = InsectGenusName.of("unobtainium-genus");
        }

        public static class Halictus {
            public static final InsectGenusName name = InsectGenusName.of("halictus");
        }

        public static class Andrena {
            public static final InsectGenusName name = InsectGenusName.of("andrena");
        }

        public static class Chrysoperla {
            public static final InsectGenusName name = InsectGenusName.of("chrysoperla");
        }

        public static class Empoasca {
            public static final InsectGenusName name = InsectGenusName.of("empoasca");
        }
    }

    public static class InsectSpecies {

        private InsectSpecies() {
        }

        /**
         * Fictitious identifiers for all entity types within this scope — guaranteed absent from any catalog.
         */
        public static class NotFound {
            public static final InsectSpeciesName name = InsectSpeciesName.of("unobtainium-beetle");
            public static final InsectImageId imageName = InsectImageId.of(
                    UUID.fromString("019dbdb7-a4a2-7eac-a875-3ce641904649"));
            public static final LifeStageName lifeStageName =
                    LifeStageName.of(name, LifeStageKind.EGG);
        }

        public static class TachinidFly {
            // tachinid-fly remains on its vernacular slug — catalog record is
            // pending (family-level, no genus/species). Migrates to binomial
            // when FU-1 ships the pending-organism mechanism.
            public static final InsectSpeciesName name = InsectSpeciesName.of("tachinid-fly");

            public static class LifeStages {
                private LifeStages() {
                }

                public static final LifeStageName Egg = LifeStageName.of(name, LifeStageKind.EGG);
                public static final LifeStageName Larva = LifeStageName.of(name, LifeStageKind.LARVA);
                public static final LifeStageName Pupa = LifeStageName.of(name, LifeStageKind.PUPA);
                public static final LifeStageName Adult = LifeStageName.of(name, LifeStageKind.ADULT);
            }
        }

        public static class BraconidWasp {
            // braconid-wasp remains on its vernacular slug — catalog record is
            // pending (family-level, no genus/species). Migrates to binomial
            // when FU-1 ships the pending-organism mechanism.
            public static final InsectSpeciesName name = InsectSpeciesName.of("braconid-wasp");

            public static class LifeStages {
                private LifeStages() {
                }

                public static final LifeStageName Egg = LifeStageName.of(name, LifeStageKind.EGG);
                public static final LifeStageName Larva = LifeStageName.of(name, LifeStageKind.LARVA);
                public static final LifeStageName Pupa = LifeStageName.of(name, LifeStageKind.PUPA);
                public static final LifeStageName Adult = LifeStageName.of(name, LifeStageKind.ADULT);
            }
        }

        public static class BattusPhilenor {
            public static final InsectSpeciesName name = InsectSpeciesName.of("battus-philenor");

            public static class LifeStages {
                private LifeStages() {
                }

                public static final LifeStageName Egg = LifeStageName.of(name, LifeStageKind.EGG);
                public static final LifeStageName Larva = LifeStageName.of(name, LifeStageKind.LARVA);
                public static final LifeStageName Pupa = LifeStageName.of(name, LifeStageKind.PUPA);
                public static final LifeStageName Adult = LifeStageName.of(name, LifeStageKind.ADULT);
            }
        }

        public static class PotatoLeafhopper {
            // potato-leafhopper remains on its vernacular slug — catalog record
            // is pending (genus-level, no species). Migrates to binomial when
            // FU-1 ships the pending-organism mechanism.
            public static final InsectSpeciesName name = InsectSpeciesName.of("potato-leafhopper");

            public static class Images {

                private Images() {
                }

                public static class Img9047 {
                    public static final InsectImageId name = InsectImageId.of(
                            UUID.fromString("0066fe0f-a3e0-70d8-b557-c42f13e67067"));
                }

                public static class Img9048 {
                    public static final InsectImageId name = InsectImageId.of(
                            UUID.fromString("6091691e-7900-7ed3-a35b-88c46e47b866"));
                }
            }
        }
    }
}
