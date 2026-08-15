package com.naturalist.garden;

import com.naturalist.plants.cultivar.CultivarName;

import java.util.UUID;

/**
 * Single source of truth for garden {@code EntityName} / {@code EntityId} constants used in
 * repository and query contract tests. Mirrors the domain object graph: a {@code CropType} is the
 * agronomic category, and its {@code Planting}s are the individual varieties in the ground. Ids
 * match the fixture JSON exactly.
 * <p>
 * Cultivar constants name plants-domain entities. Garden records that they were planted; plants
 * owns what they are.
 */
public final class TestGardenIdentifiers {

    private TestGardenIdentifiers() {
    }

    public static final class CropTypes {

        private CropTypes() {
        }

        /** Four varieties across two beds in 2026, all removed by August. */
        public static final class Tomato {

            private Tomato() {
            }

            public static final CropTypeName name = CropTypeName.of("tomato");

            public static final class Cultivars {

                private Cultivars() {
                }

                public static final CultivarName amishPaste = CultivarName.of("amish-paste");
                public static final CultivarName italianPearNicks = CultivarName.of("italian-pear-nicks");
                public static final CultivarName sanMarzanoF2 = CultivarName.of("san-marzano-f2");
                public static final CultivarName sungoldCherry = CultivarName.of("sungold-cherry");
            }

            public static final class Plantings {

                private Plantings() {
                }

                /** Twelve plants, back yard south row, April 6 2026. */
                public static final PlantingId amishPasteBackyard =
                        PlantingId.of(UUID.fromString("02671301-0000-7000-8000-000003100000"));

                /** The heirloom line, back yard centre. */
                public static final PlantingId italianPearBackyard =
                        PlantingId.of(UUID.fromString("02671301-0001-7000-8000-000003100001"));

                /** The north row, which carried the April 7 TSWV outbreak. */
                public static final PlantingId sanMarzanoBackyard =
                        PlantingId.of(UUID.fromString("02671301-0002-7000-8000-000003100002"));

                /** A single plant in Box 1 — the zone-scoped case, no sub-zone. */
                public static final PlantingId sungoldBox1 =
                        PlantingId.of(UUID.fromString("02671301-0003-7000-8000-000003100003"));
            }
        }

        /** Still in the ground: the active-planting case. */
        public static final class Basil {

            private Basil() {
            }

            public static final CropTypeName name = CropTypeName.of("basil");

            public static final class Plantings {

                private Plantings() {
                }

                public static final PlantingId genoveseBox1 =
                        PlantingId.of(UUID.fromString("02671301-0004-7000-8000-000003100004"));
            }
        }

        /**
         * Grown and soil-tested for, with no botanical identification and — as of August 2026 — no
         * planting yet. The August FGL panel is a lettuce panel for a crop still to go in, which is
         * why an analysis records the crop type it was interpreted for rather than deriving it.
         */
        public static final class Lettuce {

            private Lettuce() {
            }

            public static final CropTypeName name = CropTypeName.of("lettuce");
        }

        /**
         * The eggplant shares the back yard south row with the Amish Paste tomatoes — a mixed row,
         * and the reason a sub-zone claims nothing about what is planted in it.
         */
        public static final class Eggplant {

            private Eggplant() {
            }

            public static final CropTypeName name = CropTypeName.of("eggplant");

            public static final class Plantings {

                private Plantings() {
                }

                public static final PlantingId blackBeautyBackyard =
                        PlantingId.of(UUID.fromString("02671301-0007-7000-8000-000003100007"));
            }
        }

        public static final class NotFound {

            private NotFound() {
            }

            public static final CropTypeName cropType = CropTypeName.of("unobtainium-melon");

            public static final PlantingId planting =
                    PlantingId.of(UUID.fromString("02671399-0000-7000-8000-000003900000"));
        }
    }
}
