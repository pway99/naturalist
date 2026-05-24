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

            public static class FunctionalRole {
                public static final InsectFunctionalRoleId name = InsectFunctionalRoleId.of(
                        UUID.fromString("019e5221-b000-70ab-8000-ee00cafef00d"));
            }
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

        public static class Coccinellidae {
            public static final InsectFamilyName name = InsectFamilyName.of("coccinellidae");
        }

        public static class Ectobiidae {
            public static final InsectFamilyName name = InsectFamilyName.of("ectobiidae");
        }

        public static class Apidae {
            public static final InsectFamilyName name = InsectFamilyName.of("apidae");
        }

        public static class Nymphalidae {
            public static final InsectFamilyName name = InsectFamilyName.of("nymphalidae");
        }

        public static class Pieridae {
            public static final InsectFamilyName name = InsectFamilyName.of("pieridae");
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

            public static class FunctionalRole {
                public static final InsectFunctionalRoleId name = InsectFunctionalRoleId.of(
                        UUID.fromString("019e5221-b007-77ab-8700-ee00cafef00d"));
            }

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

        public static class Hippodamia {
            public static final InsectGenusName name = InsectGenusName.of("hippodamia");
        }

        public static class Blattella {
            public static final InsectGenusName name = InsectGenusName.of("blattella");
        }

        public static class Xylocopa {
            public static final InsectGenusName name = InsectGenusName.of("xylocopa");
        }

        public static class Vanessa {
            public static final InsectGenusName name = InsectGenusName.of("vanessa");
        }

        public static class Battus {
            public static final InsectGenusName name = InsectGenusName.of("battus");
        }

        public static class Colias {
            public static final InsectGenusName name = InsectGenusName.of("colias");
        }
    }

    public static class InsectFunctionalRole {

        private InsectFunctionalRole() {
        }

        /**
         * Fictitious identifier for the cross-rank
         * {@link com.naturalist.insects.InsectFunctionalRole} scope —
         * guaranteed absent from any catalog. The scope sits at the top
         * level rather than nested inside a rank because functional-role
         * records attach to records at any rank via {@code InsectRankName},
         * so there is no single parent rank to nest under.
         */
        public static class NotFound {
            public static final InsectFunctionalRoleId name = InsectFunctionalRoleId.of(
                    UUID.fromString("019dbdb8-aaaa-7eee-aaaa-aaaaaaaaaaaa"));
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

            public static class FunctionalRole {
                public static final InsectFunctionalRoleId name = InsectFunctionalRoleId.of(
                        UUID.fromString("019e5221-b00e-7eab-8e00-ee00cafef00d"));
            }

            public static class LifeStages {
                private LifeStages() {
                }

                public static final LifeStageName Egg = LifeStageName.of(name, LifeStageKind.EGG);
                public static final LifeStageName Larva = LifeStageName.of(name, LifeStageKind.LARVA);
                public static final LifeStageName Pupa = LifeStageName.of(name, LifeStageKind.PUPA);
                public static final LifeStageName Adult = LifeStageName.of(name, LifeStageKind.ADULT);
            }

            public static class Images {

                private Images() {
                }

                public static class PipevineSwallowtail {
                    public static final InsectImageId name = InsectImageId.of(
                            UUID.fromString("019e0f1b-71d0-7cdb-9d07-d8214037a4cb"));
                }
            }
        }
    }
}
