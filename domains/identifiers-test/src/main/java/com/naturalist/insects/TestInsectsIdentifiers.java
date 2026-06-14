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
 *     repository().getByName(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name)
 *     repository().getByName(TestInsectsIdentifiers.InsectSpecies.NotFound.name)
 * </pre>
 */
public class TestInsectsIdentifiers {

    private TestInsectsIdentifiers() {
    }

    public static class InsectOrder {

        private InsectOrder() {
        }

        /**
         * Fictitious identifier for the {@link com.naturalist.insects.InsectOrder}
         * scope — guaranteed absent from any catalog.
         */
        public static class NotFound {
            public static final InsectOrderName name = InsectOrderName.of("zygentoma");
        }

        public static class Diptera {
            public static final InsectOrderName name = InsectOrderName.of("diptera");
        }

        public static class Hymenoptera {
            public static final InsectOrderName name = InsectOrderName.of("hymenoptera");
        }

        public static class Blattodea {
            public static final InsectOrderName name = InsectOrderName.of("blattodea");
        }
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

            public static class LifeStages {
                private LifeStages() {
                }

                public static final LifeStageName Egg = LifeStageName.of(name, LifeStageKind.EGG);
                public static final LifeStageName Larva = LifeStageName.of(name, LifeStageKind.LARVA);
                public static final LifeStageName Pupa = LifeStageName.of(name, LifeStageKind.PUPA);
                public static final LifeStageName Adult = LifeStageName.of(name, LifeStageKind.ADULT);
            }
        }

        public static class Braconidae {
            public static final InsectFamilyName name = InsectFamilyName.of("braconidae");

            public static class LifeStages {
                private LifeStages() {
                }

                public static final LifeStageName Egg = LifeStageName.of(name, LifeStageKind.EGG);
                public static final LifeStageName Larva = LifeStageName.of(name, LifeStageKind.LARVA);
                public static final LifeStageName Pupa = LifeStageName.of(name, LifeStageKind.PUPA);
                public static final LifeStageName Adult = LifeStageName.of(name, LifeStageKind.ADULT);
            }
        }

        public static class Syrphidae {
            public static final InsectFamilyName name = InsectFamilyName.of("syrphidae");

            public static class FunctionalRole {
                public static final InsectFunctionalRoleId id = InsectFunctionalRoleId.of(
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

        public static class Crabronidae {
            public static final InsectFamilyName name = InsectFamilyName.of("crabronidae");
        }

        public static class Drosophilidae {
            public static final InsectFamilyName name = InsectFamilyName.of("drosophilidae");
        }

        public static class Ectobiidae {
            public static final InsectFamilyName name = InsectFamilyName.of("ectobiidae");
        }

        public static class Apidae {
            public static final InsectFamilyName name = InsectFamilyName.of("apidae");
        }

        public static class Blattidae {
            public static final InsectFamilyName name = InsectFamilyName.of("blattidae");
        }

        public static class Nymphalidae {
            public static final InsectFamilyName name = InsectFamilyName.of("nymphalidae");
        }

        public static class Pieridae {
            public static final InsectFamilyName name = InsectFamilyName.of("pieridae");
        }

        public static class Rhinotermitidae {
            public static final InsectFamilyName name = InsectFamilyName.of("rhinotermitidae");
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

        public static class Apis {
            public static final InsectGenusName name = InsectGenusName.of("apis");
        }

        public static class Chrysoperla {
            public static final InsectGenusName name = InsectGenusName.of("chrysoperla");
        }

        public static class Empoasca {
            public static final InsectGenusName name = InsectGenusName.of("empoasca");

            public static class FunctionalRole {
                public static final InsectFunctionalRoleId id = InsectFunctionalRoleId.of(
                        UUID.fromString("019e5221-b007-77ab-8700-ee00cafef00d"));
            }

            public static class Images {

                private Images() {
                }

                public static class Img9047 {
                    public static final InsectImageId id = InsectImageId.of(
                            UUID.fromString("0066fe0f-a3e0-70d8-b557-c42f13e67067"));
                }

                public static class Img9048 {
                    public static final InsectImageId id = InsectImageId.of(
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

        public static class Drosophila {
            public static final InsectGenusName name = InsectGenusName.of("drosophila");
        }

        public static class Periplaneta {
            public static final InsectGenusName name = InsectGenusName.of("periplaneta");
        }

        public static class Philanthus {
            public static final InsectGenusName name = InsectGenusName.of("philanthus");
        }

        public static class Reticulitermes {
            public static final InsectGenusName name = InsectGenusName.of("reticulitermes");
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
            public static final InsectFunctionalRoleId id = InsectFunctionalRoleId.of(
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
            public static final InsectImageId imageId = InsectImageId.of(
                    UUID.fromString("019dbdb7-a4a2-7eac-a875-3ce641904649"));
            public static final LifeStageName lifeStageName =
                    LifeStageName.of(name, LifeStageKind.EGG);
        }

        public static class ApisMellifera {
            public static final InsectSpeciesName name = InsectSpeciesName.of("apis-mellifera");

            public static class FunctionalRole {
                public static final InsectFunctionalRoleId id = InsectFunctionalRoleId.of(
                        UUID.fromString("019ec455-d43e-71de-9dab-deb3b09ad1de"));
            }
        }

        public static class BattusPhilenor {
            public static final InsectSpeciesName name = InsectSpeciesName.of("battus-philenor");

            public static class FunctionalRole {
                public static final InsectFunctionalRoleId id = InsectFunctionalRoleId.of(
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
                    public static final InsectImageId id = InsectImageId.of(
                            UUID.fromString("019e0f1b-71d0-7cdb-9d07-d8214037a4cb"));
                }
            }
        }

        public static class ColiasEurytheme {
            public static final InsectSpeciesName name = InsectSpeciesName.of("colias-eurytheme");

            public static class LifeStages {
                private LifeStages() {
                }

                public static final LifeStageName Egg = LifeStageName.of(name, LifeStageKind.EGG);
                public static final LifeStageName Larva = LifeStageName.of(name, LifeStageKind.LARVA);
                public static final LifeStageName Pupa = LifeStageName.of(name, LifeStageKind.PUPA);
                public static final LifeStageName Adult = LifeStageName.of(name, LifeStageKind.ADULT);
            }
        }

        public static class DrosophilaFunebris {
            public static final InsectSpeciesName name = InsectSpeciesName.of("drosophila-funebris");
        }

        public static class DrosophilaMelanogaster {
            public static final InsectSpeciesName name = InsectSpeciesName.of("drosophila-melanogaster");
        }

        public static class HippodamiaConvergens {
            public static final InsectSpeciesName name = InsectSpeciesName.of("hippodamia-convergens");

            public static class LifeStages {
                private LifeStages() {
                }

                public static final LifeStageName Egg = LifeStageName.of(name, LifeStageKind.EGG);
                public static final LifeStageName Larva = LifeStageName.of(name, LifeStageKind.LARVA);
                public static final LifeStageName Pupa = LifeStageName.of(name, LifeStageKind.PUPA);
                public static final LifeStageName Adult = LifeStageName.of(name, LifeStageKind.ADULT);
            }
        }

        public static class PeriplanetaAmericana {
            public static final InsectSpeciesName name = InsectSpeciesName.of("periplaneta-americana");
        }

        public static class PhilanthusGibbosus {
            public static final InsectSpeciesName name = InsectSpeciesName.of("philanthus-gibbosus");
        }

        public static class ReticulitermesHesperus {
            public static final InsectSpeciesName name = InsectSpeciesName.of("reticulitermes-hesperus");
        }
    }
}
