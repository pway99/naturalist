package com.naturalist.soil;

import com.naturalist.soil.observation.LabAnalysisId;
import com.naturalist.soil.observation.NutrientName;
import com.naturalist.soil.observation.NutrientReadingId;
import com.naturalist.soil.observation.ReportedOptimumId;
import com.naturalist.soil.observation.SoilPhysicalCharacteristicsId;

import java.util.UUID;

/**
 * Single source of truth for soil {@code EntityName} / {@code EntityId} constants used in
 * repository and query contract tests. Mirrors the domain object graph: each Oak Vista
 * {@code SoilProfile} owns a {@code LabAnalysis}, which in turn owns its {@code NutrientReading}s
 * and one {@code SoilPhysicalCharacteristics} row. Ids match the fixture JSON exactly.
 */
public final class TestSoilIdentifiers {

    private TestSoilIdentifiers() {
    }

    /** Nutrient names used as query arguments (the reading grain's secondary key). */
    public static final class Nutrients {

        private Nutrients() {
        }

        public static final NutrientName CALCIUM_SOLUBLE = NutrientName.of("calcium-soluble");
        public static final NutrientName BORON = NutrientName.of("boron");
    }

    public static final class SoilProfiles {

        private SoilProfiles() {
        }

        public static final class Box1 {

            private Box1() {
            }

            public static final SoilProfileName name = SoilProfileName.of("box1");

            public static final class LabAnalyses {

                private LabAnalyses() {
                }

                public static final LabAnalysisId labAnalysis =
                        LabAnalysisId.of(UUID.fromString("02671853-0001-7000-8000-000000000001"));
            }

            public static final class NutrientReadings {

                private NutrientReadings() {
                }

                public static final NutrientReadingId nitrateN =
                        NutrientReadingId.of(UUID.fromString("02671001-0000-7000-8000-000001000000"));
                public static final NutrientReadingId calciumSoluble =
                        NutrientReadingId.of(UUID.fromString("02671001-0005-7000-8000-000001000005"));
            }

            public static final class PhysicalCharacteristics {

                private PhysicalCharacteristics() {
                }

                public static final SoilPhysicalCharacteristicsId characteristics =
                        SoilPhysicalCharacteristicsId.of(UUID.fromString("02671901-0000-7000-8000-000000000001"));
            }

            public static final class ReportedOptima {

                private ReportedOptima() {
                }

                /** A closed range — {@code 5.3 - 7.2} on CH 2671853-001. */
                public static final ReportedOptimumId nitrateN =
                        ReportedOptimumId.of(UUID.fromString("02671101-0000-7000-8000-000001100000"));

                /** The report's one upper-bounded row — {@code < 19}. */
                public static final ReportedOptimumId sodiumSoluble =
                        ReportedOptimumId.of(UUID.fromString("02671101-0009-7000-8000-000001100009"));
            }
        }

        /**
         * The whole back yard bed — one composite FGL sample ({@code CH 2671853-002}). The
         * north / center / south sub-zones are crop rows and carry no soil profile of their own;
         * they were modelled as three profiles until the August 2026 collapse, which is why the
         * fixture ids skip {@code -0003} and {@code -0004}.
         */
        public static final class Backyard {

            private Backyard() {
            }

            public static final SoilProfileName name = SoilProfileName.of("backyard");

            public static final class LabAnalyses {

                private LabAnalyses() {
                }

                public static final LabAnalysisId labAnalysis =
                        LabAnalysisId.of(UUID.fromString("02671853-0002-7000-8000-000000000002"));
            }

            public static final class PhysicalCharacteristics {

                private PhysicalCharacteristics() {
                }

                public static final SoilPhysicalCharacteristicsId characteristics =
                        SoilPhysicalCharacteristicsId.of(UUID.fromString("02671902-0000-7000-8000-000000000002"));
            }
        }

        public static final class NotFound {

            private NotFound() {
            }

            public static final SoilProfileName soilProfile = SoilProfileName.of("unobtainium-bed");

            public static final LabAnalysisId labAnalysis =
                    LabAnalysisId.of(UUID.fromString("02671853-9999-7000-8000-000000009999"));

            public static final NutrientReadingId nutrientReading =
                    NutrientReadingId.of(UUID.fromString("02671999-0000-7000-8000-000000009999"));

            public static final SoilPhysicalCharacteristicsId physicalCharacteristics =
                    SoilPhysicalCharacteristicsId.of(UUID.fromString("02671998-0000-7000-8000-000000009998"));

            public static final ReportedOptimumId reportedOptimum =
                    ReportedOptimumId.of(UUID.fromString("02671997-0000-7000-8000-000000009997"));

            public static final NutrientName nutrientName = NutrientName.of("unobtainium");
        }
    }
}
