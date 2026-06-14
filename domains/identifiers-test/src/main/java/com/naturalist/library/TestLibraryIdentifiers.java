package com.naturalist.library;

import com.naturalist.authority.CitationName;

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

        public static class NotFound {
            public static final CitationName name =
                    CitationName.of("eol-unobtainium-bug-9999999");
        }
    }
}
