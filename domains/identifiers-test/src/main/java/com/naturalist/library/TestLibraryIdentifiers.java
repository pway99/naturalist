package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.library.CitationAssociationId;

import java.util.UUID;

public class TestLibraryIdentifiers {

    public static class Citations {

        public static final CitationName EolSwallowtail =
                CitationName.of("eol-battus-philenor-1188585");

        public static final CitationName EolGreenLacewing =
                CitationName.of("eol-chrysoperla-rufilabris-2774541");

        public static final CitationName EolMonarchButterfly =
                CitationName.of("eol-danaus-plexippus-151935");

        public static final CitationName EolHoneyBee =
                CitationName.of("eol-apis-mellifera-1045608");

        public static final CitationName EolFruitFly =
                CitationName.of("eol-drosophila-melanogaster-733739");

        public static final CitationName EolFunebris =
                CitationName.of("eol-drosophila-funebris-733824");

        public static final CitationName EolAmericanCockroach =
                CitationName.of("eol-periplaneta-americana-1076920");

        public static final CitationName EolWesternTermite =
                CitationName.of("eol-reticulitermes-hesperus-469438");

        public static final CitationName EolBeewolf =
                CitationName.of("eol-philanthus-gibbosus-104130");

        public static class Associations {

            private Associations() {
            }

            public static final CitationAssociationId EolSwallowtailOnLepidoptera =
                    CitationAssociationId.of(
                            UUID.fromString("019f0001-a001-7001-8001-a00000000001"));

            public static final CitationAssociationId EolSwallowtailOnPapilionidae =
                    CitationAssociationId.of(
                            UUID.fromString("019f0001-a002-7002-8002-a00000000002"));

            public static final CitationAssociationId EolMonarchOnDiptera =
                    CitationAssociationId.of(
                            UUID.fromString("019f0001-a003-7003-8003-a00000000003"));

            public static final CitationAssociationId EolHoneyBeeOnHymenoptera =
                    CitationAssociationId.of(
                            UUID.fromString("019f0001-a004-7004-8004-a00000000004"));

            public static class NotFound {
                public static final CitationAssociationId name =
                        CitationAssociationId.of(
                                UUID.fromString("019f0001-ffff-7fff-bfff-ffffffffffff"));
            }
        }

        public static class NotFound {
            public static final CitationName name =
                    CitationName.of("eol-unobtainium-bug-9999999");
        }
    }

    public static class Concepts {

        public static final ConceptName Clade = ConceptName.of("clade");

        public static final ConceptName CladeTaxonomyRelation =
                ConceptName.of("clade-taxonomy-relation");

        public static class NotFound {
            public static final ConceptName name = ConceptName.of("unobtainium-concept");
        }
    }
}
