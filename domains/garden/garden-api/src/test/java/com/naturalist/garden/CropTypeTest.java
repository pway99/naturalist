package com.naturalist.garden;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CropTypeTest {

    private static final Observer observer = Observer.forClass(CropTypeTest.class);

    @Test
    void typeWithASpeciesHasNoViolations() {
        var mo = observer.forMethod("typeWithASpeciesHasNoViolations");
        var tomato = new CropType(CropTypeName.of("tomato"), PlantName.of("solanum-lycopersicum"));

        InvariantObservation result = mo.namedEntity(tomato, "cropType");

        assertThat(result.violations()).isEmpty();
        assertThat(tomato.isBotanicallyIdentified()).isTrue();
    }

    /**
     * Lettuce is grown and soil-tested for without anyone deciding which <em>Lactuca</em> it is.
     * That is the normal state of a crop type, not a defect, so the botanical link is optional.
     */
    @Test
    void typeWithoutASpeciesHasNoViolations() {
        var mo = observer.forMethod("typeWithoutASpeciesHasNoViolations");
        var lettuce = new CropType(CropTypeName.of("lettuce"), null);

        InvariantObservation result = mo.namedEntity(lettuce, "cropType");

        assertThat(result.violations()).isEmpty();
        assertThat(lettuce.isBotanicallyIdentified()).isFalse();
    }

    @Test
    void missingNameIsRejected() {
        var mo = observer.forMethod("missingNameIsRejected");

        InvariantObservation result = mo.namedEntity(new CropType(null, null), "cropType");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".cropType.name");
    }
}
