package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class OrganismObservationQueryImplTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectQuery.FieldObservationQuery query =
            new OrganismObservationQueryImpl(new OrganismObservationRepositoryMock(db));

    @Test
    void forNaturalist_returnsOnlyThatNaturalistsObservations() {
        var patrick = query.forNaturalist(NaturalistName.of("patrick-way"));
        assertThat(patrick.isEmpty()).isFalse();
        assertThat(patrick.stream()).allMatch(o ->
                o.observedBy().equals(NaturalistName.of("patrick-way")));
    }

    @Test
    void forNaturalist_unknownNaturalist_isEmpty() {
        assertThat(query.forNaturalist(NaturalistName.of("nobody-here")).isEmpty()).isTrue();
    }

    @Test
    void forNaturalistAndSubjects_restrictsToGivenRanks() {
        var battus = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var result = query.forNaturalistAndSubjects(NaturalistName.of("patrick-way"), java.util.Set.of(battus));
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.stream()).allMatch(o -> o.subject().equals(battus)
                && o.observedBy().equals(NaturalistName.of("patrick-way")));
    }

    @Test
    void forNaturalistAndSubjects_emptyRankSet_isEmpty() {
        assertThat(query.forNaturalistAndSubjects(
                NaturalistName.of("patrick-way"), java.util.Set.<InsectRankName>of()).isEmpty()).isTrue();
    }
}
