package com.naturalist.plants;

import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlantFeatureViewTest {

    private final Observer observer = Observer.forClass(PlantFeatureViewTest.class);

    @Test
    void validView_hasNoViolations() {
        PlantFeature ray = PlantFeature.of(PlantFeatureId.create(), "ray florets");
        PlantFeatureView view = new PlantFeatureView(
                PlantGenusName.of("helianthus"),
                List.of(new PlantFeatureView.RankGroup(PlantFamilyName.of("asteraceae"), List.of(ray))));
        assertThat(observer.forMethod("valid").observable(view, "view").violations()).isEmpty();
    }

    @Test
    void nullSubject_reportsViolation() {
        var mo = observer.forMethod("null");
        PlantFeatureView view = new PlantFeatureView(null, List.of());
        assertThat(mo.observable(view, "view").violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".view.subject");
    }
}
