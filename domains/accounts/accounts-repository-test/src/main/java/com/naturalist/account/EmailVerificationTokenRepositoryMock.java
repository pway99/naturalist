package com.naturalist.account;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.Optional;

/**
 * In-memory {@link AccountRepository.VerificationTokenRepository} backed by
 * {@link EmailVerificationTokenTestEntitySource}. Marked {@link MockDomainService} for the
 * {@code mock-data} runtime bridge (ADR-025).
 */
@MockDomainService
class EmailVerificationTokenRepositoryMock
        extends AbstractTestEntityRepository<
                        EmailVerificationTokenId,
                        EmailVerificationToken,
                        EmailVerificationTokenTestEntitySource>
        implements AccountRepository.VerificationTokenRepository {

    EmailVerificationTokenRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Optional<EmailVerificationToken> getByTokenHash(String tokenHash) {
        MethodObserver getByTokenHash = observer().forMethod("getByTokenHash");
        getByTokenHash.arguments(i -> i.notBlank(tokenHash, "tokenHash"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(token -> tokenHash.equals(token.tokenHash()))
                .findFirst();
        getByTokenHash.state(s -> s.namedEntityOrNull(result.orElse(null), "result")).observe(Level.WARN);
        return result;
    }
}
