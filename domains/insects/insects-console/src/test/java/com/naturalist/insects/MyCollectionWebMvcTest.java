package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.PageRequest;
import com.naturalist.data.Pages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The species listing filters to the signed-in naturalist's observed species when
 * the collection lens is on, and shows the full catalog otherwise. A DB-free,
 * scan-free, security-free {@code @WebMvcTest} slice ({@link InsectsControllerTestConfig});
 * the current naturalist is the {@code naturalist.currentNaturalistName} request
 * attribute. {@code patrick-way} has observed battus-philenor but not apis-mellifera.
 */
@WebMvcTest
class MyCollectionWebMvcTest {

    @RegisterExtension
    static final NaturalistTestExtension database = InsectsControllerTestConfig.DATABASE;

    private static final String CURRENT_NATURALIST = "naturalist.currentNaturalistName";

    @Autowired
    MockMvc mockMvc;

    @Test
    void lensOn_filtersToObservedSpecies() throws Exception {
        mockMvc.perform(get("/insects/species")
                        .sessionAttr("insects.collectionLens", true)
                        .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("battus-philenor")))
                .andExpect(content().string(not(containsString("apis-mellifera"))));
    }

    @Test
    void lensOff_showsFullCatalog() throws Exception {
        mockMvc.perform(get("/insects/species").requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("apis-mellifera")));
    }

    @Test
    void mineParam_isIgnored_afterRetirement() throws Exception {
        // ?mine=true must no longer filter -- only the session lens does.
        mockMvc.perform(get("/insects/species").param("mine", "true")
                        .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("apis-mellifera")));
    }

    @Test
    void emptyCollection_showsNudge() throws Exception {
        // A naturalist with no seeded observations, lens on -> the empty-collection nudge.
        mockMvc.perform(get("/insects/species")
                        .sessionAttr("insects.collectionLens", true)
                        .requestAttr(CURRENT_NATURALIST, "empty-collector"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No insects in your collection yet")));
    }

    @Test
    void mine_withManyCollectedSpecies_pagesInsteadOfEmptying() throws Exception {
        // More collected species than one console page: the lens pages over the naturalist's
        // collection, so page 1 holds the overflow instead of an empty filter over catalogue page 1.
        InsectsTestContext insects = InsectsControllerTestConfig.INSECTS;
        InsectSpecies template = insects.insectQuery().species()
                .findPage(PageRequest.first(1)).content().getFirst();
        // Enough species for three catalogue pages.
        for (int i = 0; i < 2 * PageRequest.DEFAULT_CONSOLE_PAGE_SIZE; i++) {
            insects.insectCommand().species().insert(new InsectSpecies(
                    InsectSpeciesName.of("zz-extra-" + i), template.genusName(), template.epithet(),
                    template.description(), template.commonNames(), null, null, null, null, null,
                    null, null, null, null));
        }
        // Collect every species except those on catalogue page 1 -- still more than one page --
        // so a lens that merely filters catalogue page 1 renders it empty.
        java.util.Set<InsectSpeciesName> catalogPageOne = insects.insectQuery().species()
                .findPage(PageRequest.console(1)).content().stream()
                .map(InsectSpecies::name).collect(java.util.stream.Collectors.toSet());
        List<InsectSpecies> collected = Pages.stream(PageRequest.MAX_PAGE_SIZE,
                insects.insectQuery().species()::findPage)
                .filter(species -> !catalogPageOne.contains(species.name())).toList();
        assertThat(collected).hasSizeGreaterThan(PageRequest.DEFAULT_CONSOLE_PAGE_SIZE);
        for (InsectSpecies species : collected) {
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/insects/" + species.name().value() + "/observe")
                            .requestAttr(CURRENT_NATURALIST, "page-collector"))
                    .andExpect(status().is3xxRedirection());
        }

        mockMvc.perform(get("/insects/species").param("page", "1")
                        .sessionAttr("insects.collectionLens", true)
                        .requestAttr(CURRENT_NATURALIST, "page-collector"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/life-stages\"")))
                .andExpect(content().string(not(containsString("No insects in your collection yet"))));
    }
}
