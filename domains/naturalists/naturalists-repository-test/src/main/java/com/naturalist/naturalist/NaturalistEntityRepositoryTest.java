package com.naturalist.naturalist;

import com.naturalist.RandomValue;
import com.naturalist.account.AccountName;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link NaturalistRepository.NaturalistEntityRepository}.
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002), plus the
 * {@code getByAccount} finder cases. The known constants match the fixtures in
 * {@code naturalists/naturalists.json}.
 */
interface NaturalistEntityRepositoryTest
        extends EntityRepositoryTest<NaturalistName, Naturalist> {

    NaturalistName PATRICK = NaturalistName.of("patrick-way");
    NaturalistName DELIA = NaturalistName.of("delia-durrell");
    NaturalistName NOT_FOUND = NaturalistName.of("nobody-here");

    @Override
    NaturalistRepository.NaturalistEntityRepository repository();

    @Override
    default TestEntitySource<NaturalistName, Naturalist> source() {
        return db.getNamed(NaturalistTestEntitySource.class);
    }

    @Override
    default NaturalistName notFoundName() {
        return NOT_FOUND;
    }

    @Override
    default List<NaturalistName> knownEntityNames() {
        return List.of(PATRICK, DELIA);
    }

    @Override
    default Naturalist newEntity() {
        return new Naturalist(
                NaturalistName.of(RandomValue.string()),
                AccountName.of("acct-" + RandomValue.string()),
                "Handle" + RandomValue.string(),
                "Given" + RandomValue.string(),
                "Family" + RandomValue.string(),
                NaturalistRole.VISITOR,
                EcologicalStage.WONDER,
                "notes " + RandomValue.string());
    }

    @Override
    default Naturalist ghostEntity() {
        return new Naturalist(
                NaturalistName.of(RandomValue.string()),
                AccountName.of("acct-" + RandomValue.string()),
                "Handle" + RandomValue.string(),
                "Given" + RandomValue.string(),
                null,
                NaturalistRole.VISITOR,
                EcologicalStage.WONDER,
                null);
    }

    @Override
    default Naturalist modifiedEntity(Naturalist original) {
        return new Naturalist(
                original.name(),
                original.account(),
                "ChangedHandle" + RandomValue.string(),
                "Changed" + RandomValue.string(),
                "Changed" + RandomValue.string(),
                NaturalistRole.KEEPER,
                EcologicalStage.NATURALIST,
                "changed " + RandomValue.string());
    }

    @Test
    default void getByAccount_rejectsNull() {
        assertThatThrownBy(() -> repository().getByAccount(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("account");
    }

    @Test
    default void getByAccount_returnsNaturalistWithMatchingAccount() {
        var result = repository().getByAccount(AccountName.of("acct-018f3a2e9b71"));

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo(PATRICK);
    }

    @Test
    default void getByAccount_returnsEmptyForUnknownAccount() {
        assertThat(repository().getByAccount(AccountName.of("acct-nobody"))).isEmpty();
    }
}
