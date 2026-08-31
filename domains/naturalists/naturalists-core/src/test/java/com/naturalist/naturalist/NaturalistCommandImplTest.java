package com.naturalist.naturalist;

import com.naturalist.account.AccountName;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link NaturalistCommandImpl}. Wires the command over the in-memory
 * mock ({@code naturalists.json} seeds five naturalists) so {@code create} and {@code rename}
 * are exercised without any RDBMS dependency.
 */
class NaturalistCommandImplTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    private final NaturalistRepository.NaturalistEntityRepository naturalists =
            new NaturalistEntityRepositoryMock(nte);

    private final NaturalistCommand command = new NaturalistCommandImpl(naturalists);

    @Test
    void create_persistsNaturalistAndReturnsMintedName() {
        NaturalistName minted = command.create(
                AccountName.of("acct-018f3a2ef533"),
                "Brand New Handle",
                "Brand",
                "New",
                NaturalistRole.VISITOR,
                EcologicalStage.WONDER,
                "fresh notes");

        assertThat(minted.isValid()).isTrue();
        Naturalist stored = naturalists.getByName(minted).orElseThrow();
        assertThat(stored.account()).isEqualTo(AccountName.of("acct-018f3a2ef533"));
        assertThat(stored.publicHandle()).isEqualTo("Brand New Handle");
        assertThat(stored.givenName()).isEqualTo("Brand");
        assertThat(stored.familyName()).isEqualTo("New");
        assertThat(stored.role()).isEqualTo(NaturalistRole.VISITOR);
        assertThat(stored.stage()).isEqualTo(EcologicalStage.WONDER);
        assertThat(stored.notes()).isEqualTo("fresh notes");
    }

    @Test
    void create_rejectsBlankPublicHandle() {
        assertThatThrownBy(() -> command.create(
                AccountName.of("acct-018f3a2ef533"), "  ", "Given", null,
                NaturalistRole.VISITOR, EcologicalStage.WONDER, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("publicHandle");
    }

    @Test
    void create_rejectsNullAccount() {
        assertThatThrownBy(() -> command.create(
                null, "Handle", "Given", null,
                NaturalistRole.VISITOR, EcologicalStage.WONDER, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("account");
    }

    @Test
    void rename_changesPublicHandle() {
        // patrick-way is seeded with publicHandle "Patrick Way"
        command.rename(NaturalistName.of("patrick-way"), "Pat Way");

        Naturalist stored = naturalists.getByName(NaturalistName.of("patrick-way")).orElseThrow();
        assertThat(stored.publicHandle()).isEqualTo("Pat Way");
        // everything else is untouched
        assertThat(stored.givenName()).isEqualTo("Patrick");
        assertThat(stored.role()).isEqualTo(NaturalistRole.CARETAKER);
    }

    @Test
    void rename_rejectsBlankNewPublicHandle() {
        assertThatThrownBy(() -> command.rename(NaturalistName.of("patrick-way"), ""))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("newPublicHandle");
    }

    @Test
    void rename_rejectsUnknownNaturalist() {
        assertThatThrownBy(() -> command.rename(NaturalistName.of("nobody-here"), "New Handle"))
                .isInstanceOf(NoSuchElementException.class);
    }
}
