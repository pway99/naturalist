package com.naturalist.authority.eol;

import com.naturalist.authority.AuthorityReference;
import com.naturalist.ddd.EntityName;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EolClientMockTest {

    @RegisterExtension
    NaturalistTestExtension db = NaturalistTestExtension.create();

    EolClientMock client = new EolClientMock(db);

    private static final TestSubjectName SWALLOWTAIL = new TestSubjectName("battus-philenor");
    private static final TestSubjectName UNKNOWN = new TestSubjectName("unobtainium-bug");

    private static AuthorityReference swallowtailRef() {
        return new AuthorityReference(Eol.SOURCE, Eol.deepLink(new EolPageId("1188585")));
    }

    @Test
    void sourceIsEol() {
        assertThat(client.source()).isEqualTo(Eol.SOURCE);
    }

    @Test
    void knownSubjectReturnsSeededReferences() {
        assertThat(client.lookup(SWALLOWTAIL)).containsExactly(swallowtailRef());
    }

    @Test
    void unknownSubjectReturnsEmpty() {
        assertThat(client.lookup(UNKNOWN)).isEmpty();
    }

    @Test
    void nullSubjectIsRejected() {
        assertThatThrownBy(() -> client.lookup(null))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void lookupMatchesBySlugAcrossEntityNameSubtypes() {
        Set<AuthorityReference> fromTestName = client.lookup(SWALLOWTAIL);
        Set<AuthorityReference> fromOtherName = client.lookup(new AnotherSubjectName("battus-philenor"));

        assertThat(fromTestName).isEqualTo(fromOtherName);
    }

    /** Test-only EntityName subtype — keeps external-authorities free of any domain dependency. */
    private static final class TestSubjectName extends EntityName {
        private TestSubjectName(String value) {
            super(value);
        }

        @Override
        protected int maxLength() {
            return 100;
        }
    }

    /** Second EntityName subtype to verify slug-based matching works across types. */
    private static final class AnotherSubjectName extends EntityName {
        private AnotherSubjectName(String value) {
            super(value);
        }

        @Override
        protected int maxLength() {
            return 100;
        }
    }
}
