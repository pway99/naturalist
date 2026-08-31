package com.naturalist.naturalist;

import com.naturalist.RandomValue;
import com.naturalist.account.AccountName;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;

import java.util.List;

/**
 * Behavioral contract for {@link NaturalistRepository.NaturalistEntityRepository}.
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
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
}
