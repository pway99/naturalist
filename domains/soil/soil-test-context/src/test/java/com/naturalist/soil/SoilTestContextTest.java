package com.naturalist.soil;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class SoilTestContextTest {

    @RegisterExtension
    final NaturalistTestExtension db = NaturalistTestExtension.create();

    @Test
    void assemblesBox1WithItsAnalysis() {
        SoilTestContext context = SoilTestContext.create(db);

        var profile = context.soilProfileQuery().getBySoilProfileName(SoilProfileName.of("box1"));

        assertThat(profile).isPresent();
        assertThat(profile.get().labAnalyses()).isNotEmpty();
        assertThat(context.soilProfileInfoQuery().findPage(PageRequest.console(0)).content())
                .isNotEmpty();
    }
}
