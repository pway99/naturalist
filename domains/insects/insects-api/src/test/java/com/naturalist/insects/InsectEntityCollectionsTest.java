package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.FunctionalRoleCollection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectEntityCollectionsTest {

    @Test
    void sortedByParentName_isAlphabeticalBySlug() {
        FunctionalRoleCollection roles = FunctionalRoleCollection.of(List.of(
                role(InsectSpeciesName.of("danaus-plexippus")),
                role(InsectFamilyName.of("apidae")),
                role(InsectGenusName.of("battus"))));

        assertThat(roles.sortedByParentName().stream().map(r -> r.parentName().value()))
                .containsExactly("apidae", "battus", "danaus-plexippus");
    }

    private static InsectFunctionalRole role(InsectRankName parent) {
        return new InsectFunctionalRole(
                InsectFunctionalRoleId.create(), parent, Set.of(FunctionalGuild.POLLINATOR), true);
    }
}
