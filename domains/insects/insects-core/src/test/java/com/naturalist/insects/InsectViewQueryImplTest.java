package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.ImageUpload;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectViewQueryImplTest {
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1};

    private static final NaturalistName FLORA = NaturalistName.of("flora-mendez");

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    InsectsTestContextInternal context = InsectsTestContextInternal.create(nte);

    InsectViewQuery views = context.insectViewQuery();

    InsectRankName species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;

    @Test
    void rankPage_noViewer_carriesTheSameInsectAsGetByName() {
        InsectViews.RankPage page = views.rankPage(species, null).orElseThrow();

        assertThat(page.insect()).isEqualTo(context.insectQuery().getByName(species).orElseThrow());
        assertThat(page.viewer()).isEqualTo(InsectViews.Viewer.anonymous());
    }

    @Test
    void rankPage_viewerWithoutObservations() {
        InsectViews.RankPage page = views.rankPage(species, FLORA).orElseThrow();

        assertThat(page.viewer().collected()).isFalse();
        assertThat(page.viewer().ownImages().stream().toList()).isEmpty();
        assertThat(page.viewer().ownObservationByImage()).isEmpty();
        assertThat(page.insect().observations().stream().toList())
                .isEqualTo(context.insectQuery().getByName(species).orElseThrow().observations().stream().toList());
    }

    @Test
    void rankPage_viewerWithPhotoObservation() {
        context.insectCommand().photos().add(species, ImageUpload.of(JPEG), FLORA, "on the pipevine", null);

        InsectViews.RankPage page = views.rankPage(species, FLORA).orElseThrow();

        InsectViews.Viewer viewer = page.viewer();
        assertThat(viewer.collected()).isTrue();
        assertThat(viewer.ownImages().stream().toList()).hasSize(1);
        var image = viewer.ownImages().stream().findFirst().orElseThrow();
        assertThat(viewer.ownObservationByImage()).containsOnlyKeys(image.id());
        assertThat(viewer.ownObservationByImage().get(image.id()).observedBy()).isEqualTo(FLORA);
        assertThat(viewer.ownObservationByImage().get(image.id()).id()).isEqualTo(image.observationId());
        assertThat(page.insect().observations().stream().toList()).contains(image);
        assertThat(views.rankPage(species, NaturalistName.of("someone-else")).orElseThrow().viewer().collected()).isFalse();
    }

    @Test
    void rankPage_unknownName_isEmpty() {
        assertThat(views.rankPage(TestInsectsIdentifiers.InsectSpecies.NotFound.name, FLORA)).isEmpty();
    }

    @Test
    void rankPage_rejectsNullName() {
        assertThatThrownBy(() -> views.rankPage(null, FLORA)).isInstanceOf(InvariantViolationException.class);
    }
}
