package com.naturalist.plants;

import com.naturalist.plants.management.PlantProgramName;

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

    private TestPlantsIdentifiers() {}

    public static class Plants {

        private Plants() {}

        /** Fictitious identifiers for all entity types within this scope — guaranteed absent from any catalog. */
        public static class NotFound {
            public static final PlantName name = PlantName.of("unobtainium-vine");
            public static final PlantProgramName programName =
                    PlantProgramName.of("unobtainium-program");
        }

        public static class CaliforniaPipevine {
            public static final PlantName name = PlantName.of("california-pipevine");

            public static class Programs {
                private Programs() {}
                public static final PlantProgramName PesticideExclusion =
                        PlantProgramName.of("pipevine-pesticide-exclusion");
                public static final PlantProgramName LarvalMonitoring =
                        PlantProgramName.of("pipevine-larval-monitoring");
            }
        }

        public static class Borage {
            public static final PlantName name = PlantName.of("borage");

            public static class Programs {
                private Programs() {}
                public static final PlantProgramName VolunteerThinning =
                        PlantProgramName.of("borage-volunteer-thinning");
            }
        }
    }
}
