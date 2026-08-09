package com.naturalist.soil;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.PageRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoilTestContextTest {

    @Test
    void assemblesBox1WithItsAnalysis() {
        SoilTestContext context = SoilTestContext.create(NaturalistDatabase.create());

        var profile = context.soilProfileQuery().getBySoilProfileName(SoilProfileName.of("box1"));

        assertThat(profile).isPresent();
        assertThat(profile.get().labAnalyses()).isNotEmpty();
        assertThat(context.soilProfileInfoQuery().findPage(PageRequest.console(0)).content())
                .isNotEmpty();
    }
}
