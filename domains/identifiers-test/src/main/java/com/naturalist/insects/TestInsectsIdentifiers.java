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
            public static final InsectSpeciesName name = InsectSpeciesName.of("trichopoda-pennipes");

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
            public static final InsectSpeciesName name = InsectSpeciesName.of("cotesia-congregata");

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
            public static final InsectSpeciesName name = InsectSpeciesName.of("empoasca-fabae");

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
