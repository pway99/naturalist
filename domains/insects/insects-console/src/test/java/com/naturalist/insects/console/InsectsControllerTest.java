package com.naturalist.insects.console;

import com.naturalist.insects.InsectSpecies;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsControllerTest {

    @Test
    void list_addsInsectSpeciesEntitiesToModel() {
        InsectsController controller = new InsectsController();
        Model model = new ExtendedModelMap();

        controller.list(model);

        List<?> species = (List<?>) model.getAttribute("species");
        assertThat(species)
                .isNotEmpty()
                .allSatisfy(s -> assertThat(s).isInstanceOf(InsectSpecies.class));
    }

    @Test
    void list_modelEntriesUsableAsInsectSpecies() {
        InsectsController controller = new InsectsController();
        Model model = new ExtendedModelMap();
        controller.list(model);

        @SuppressWarnings("unchecked")
        List<InsectSpecies> species = (List<InsectSpecies>) model.getAttribute("species");

        // Mirrors insects/list.jte line 15 (${s.name().value()}) — the element-access
        // site where the production ClassCastException surfaces.
        for (InsectSpecies s : species) {
            s.name().value();
        }
    }
}
