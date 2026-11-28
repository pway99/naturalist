package com.naturalist.account;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final NaturalistDatabase database;

    EmailVerificationTokenRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
        this.database = naturalistDatabase;
    }

    @Override
    public Optional<EmailVerificationToken> getByTokenHash(String tokenHash) {
        observer().arguments("getByTokenHash", i -> i.notBlank(tokenHash, "tokenHash"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(token -> tokenHash.equals(token.tokenHash()))
                .findFirst();
    }

    @Override
    public int deleteForUnverifiedAccounts(Set<AccountName> accounts) {
        observer().arguments("deleteForUnverifiedAccounts", i -> i.entityNameCollection(accounts, "accounts")).throwWhenInvalid();
        Set<AccountName> unverified = database.getNamed(AccountTestEntitySource.class)
                .getByEntityNameSet(accounts).stream()
                .filter(account -> !account.emailVerified())
                .map(Account::name)
                .collect(Collectors.toUnmodifiableSet());
        Set<EmailVerificationTokenId> owned = testEntitySource().entityStream()
                .filter(token -> unverified.contains(token.account()))
                .map(EmailVerificationToken::id)
                .collect(Collectors.toUnmodifiableSet());
        return testEntitySource().delete(owned);
    }
}
