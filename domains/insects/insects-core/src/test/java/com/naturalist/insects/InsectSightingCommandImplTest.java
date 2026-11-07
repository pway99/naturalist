package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.test.context.Wired;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectSightingCommandImplTest {

    private static final NaturalistName PAT = NaturalistName.of("pat-way");
    private static final NaturalistName MALLORY = NaturalistName.of("mallory");

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Wired InsectCommand.SightingCommand sightings;
    @Wired InsectQuery insectQuery;
    InsectRankName species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;

    private InsectObservationId recordedBy(NaturalistName who, String notes) {
        return sightings.record(species, who, notes);
    }

    private String notesOf(InsectObservationId id) {
        return insectQuery.observations().getByName(id).orElseThrow().notes();
    }

    @Test
    void record_insertsObservationForNaturalist() {
        var id = recordedBy(PAT, "seen today");

        var observation = insectQuery.observations().getByName(id);
        assertThat(observation).isPresent();
        assertThat(observation.get().observedBy()).isEqualTo(PAT);
        assertThat(observation.get().subject()).isEqualTo(species);
        assertThat(observation.get().notes()).isEqualTo("seen today");
        assertThat(observation.get().identification()).isNull();
    }

    @Test
    void record_blankNotesStoredAsNull() {
        assertThat(notesOf(recordedBy(PAT, "  "))).isNull();
    }

    @Test
    void record_rejectsNullNaturalist() {
        assertThatThrownBy(() -> sightings.record(species, null, "x"))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void updateNotes_ownerChangesNotes() {
        var id = recordedBy(PAT, "old");

        sightings.updateNotes(id, PAT, "new");

        assertThat(notesOf(id)).isEqualTo("new");
    }

    @Test
    void updateNotes_blankClearsNotes() {
        var id = recordedBy(PAT, "old");

        sightings.updateNotes(id, PAT, "   ");

        assertThat(notesOf(id)).isNull();
    }

    @Test
    void updateNotes_nonOwnerLeavesNotesUnchanged() {
        var id = recordedBy(PAT, "old");

        assertThatCode(() -> sightings.updateNotes(id, MALLORY, "x")).doesNotThrowAnyException();

        assertThat(notesOf(id)).isEqualTo("old");
    }

    @Test
    void updateNotes_missingObservationIsSilent() {
        assertThatCode(() -> sightings.updateNotes(InsectObservationId.create(), PAT, "x"))
                .doesNotThrowAnyException();
    }

    @Test
    void updateNotes_rejectsNullViewer() {
        var id = recordedBy(PAT, "old");

        assertThatThrownBy(() -> sightings.updateNotes(id, null, "x"))
                .isInstanceOf(InvariantViolationException.class);
    }
}
