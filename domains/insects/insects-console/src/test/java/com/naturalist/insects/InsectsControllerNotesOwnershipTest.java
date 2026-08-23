package com.naturalist.insects;

import com.naturalist.observation.OrganismObservation;
import com.naturalist.insects.InsectObservationId;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link InsectsController#updateNotes} loads a {@link OrganismObservation} by id
 * and must refuse the update unless the signed-in naturalist is the one who
 * recorded it — otherwise naturalist A can overwrite naturalist B's field
 * notes by POSTing B's (UUIDv7, partially guessable) observation id.
 * <p>
 * {@link InsectsController#owns} is package-private specifically so this
 * decision can be unit-tested directly. {@code InsectsController} builds its
 * own private, unshared {@code NaturalistDatabase} in its constructor with no
 * externally-visible seam for pre-seeding a known {@code OrganismObservation<InsectObservationId, InsectRankName>}, so
 * an end-to-end HTTP-level test of {@code updateNotes} itself is not
 * practical here; this test covers the exact gating logic that handler
 * delegates to instead.
 */
class InsectsControllerNotesOwnershipTest {

    private static final NaturalistName PAT = NaturalistName.of("pat-way");
    private static final NaturalistName MALLORY = NaturalistName.of("mallory");

    private static OrganismObservation<InsectObservationId, InsectRankName> observationBy(NaturalistName owner) {
        return new OrganismObservation<InsectObservationId, InsectRankName>(
                InsectObservationId.create(),
                owner,
                InsectFamilyName.of("chrysomelidae"),
                Instant.parse("2026-07-16T01:54:24Z"),
                null,
                null,
                null);
    }

    @Test
    void owns_trueWhenViewerIsTheRecordingNaturalist() {
        var obs = observationBy(PAT);

        assertThat(InsectsController.owns(obs, Optional.of(PAT))).isTrue();
    }

    @Test
    void owns_falseWhenViewerIsADifferentNaturalist() {
        var obs = observationBy(PAT);

        assertThat(InsectsController.owns(obs, Optional.of(MALLORY))).isFalse();
    }

    @Test
    void owns_falseWhenNoNaturalistIsSignedIn() {
        var obs = observationBy(PAT);

        assertThat(InsectsController.owns(obs, Optional.empty())).isFalse();
    }
}
