package com.naturalist.authority.eol;

import com.naturalist.authority.AuthorityContent;
import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.ExternalAuthority;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.ddd.EntityName;
import com.naturalist.observability.Observer;
import com.naturalist.resilience.ResilienceExempt;

import java.util.Set;

/**
 * Fixture EOL client: an in-memory {@link ExternalAuthority} backed by
 * an {@link EolAuthorityTestEntitySource} resolved from the shared
 * {@link NaturalistDatabase}. The Phase-1 stand-in for the real EOL
 * HTTP client.
 */
@ResilienceExempt(reason = "in-memory fixture client; performs no I/O")
public final class EolClientMock implements ExternalAuthority {

    private static final Observer observer = Observer.forClass(EolClientMock.class);

    private final NaturalistDatabase database;

    public EolClientMock(NaturalistDatabase database) {
        this.database = database;
    }

    @Override
    public AuthoritySource source() {
        return Eol.SOURCE;
    }

    @Override
    public Set<AuthorityReference> lookup(EntityName subject) {
        observer.arguments("lookup", a -> a.notNull(subject, "subject")).throwWhenInvalid();
        return entitySource().entityStream()
                .filter(entry -> entry.name().equals(subject.value()))
                .findFirst()
                .map(entry -> Set.of(new AuthorityReference(Eol.SOURCE, Eol.deepLink(entry.pageId()))))
                .orElse(Set.of());
    }

    @Override
    public AuthorityContent fetchContent(AuthorityReference ref) {
        return new AuthorityContent(ref,
                "Stub authority content for " + ref.url()
                + ". This is placeholder text from the EOL mock client.");
    }

    private EolAuthorityTestEntitySource entitySource() {
        return database.getNamed(EolAuthorityTestEntitySource.class);
    }
}
