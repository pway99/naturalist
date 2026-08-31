package com.naturalist.account;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

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
        observer().arguments("getByTokenHash", i -> i.notBlank(tokenHash, "tokenHash"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(token -> tokenHash.equals(token.tokenHash()))
                .findFirst();
    }
}
