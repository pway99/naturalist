package com.naturalist.plants;

/**
 * Hardcoded EntityName constants for deterministic plants repository test authoring.
 * <p>
 * Class structure mirrors the domain object graph. Every entity type defines at least
 * two known names (for single-entity and set-based lookup tests) and a {@code NotFound}
 * inner class containing a fictitious name guaranteed absent from any JSON catalog.
 * <p>
 * Usage in contract tests:
 * <pre>
 *     repository().getByName(TestPlantsIdentifiers.Plants.CaliforniaPipevine.name)
 *     repository().getByName(TestPlantsIdentifiers.Plants.NotFound.name)
 * </pre>
 */
public class TestPlantsIdentifiers {

    private TestPlantsIdentifiers() {}

    public static class Plants {

        private Plants() {}

        public static final PlantName CaliforniaPipevine = PlantName.of("california-pipevine");
        public static final PlantName Borage = PlantName.of("borage");

        public static class NotFound {
            public static final PlantName name = PlantName.of("unobtainium-vine");
        }
    }
}
