package com.naturalist.account;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * In-memory {@link AccountRepository.AccountEntityRepository} backed by
 * {@link AccountTestEntitySource}. Marked {@link MockDomainService} so the Spring runtime
 * bridge registers it under the {@code mock-data} profile (ADR-025).
 */
@MockDomainService
class AccountRepositoryMock
        extends AbstractTestEntityRepository<AccountName, Account, AccountTestEntitySource>
        implements AccountRepository.AccountEntityRepository {

    private final NaturalistDatabase database;

    AccountRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
        this.database = naturalistDatabase;
    }

    @Override
    public Optional<Account> getByEmail(String email) {
        MethodObserver getByEmail = observer().forMethod("getByEmail");
        getByEmail.arguments(i -> i.email(email, "email"))
                .throwWhenInvalid();
        Optional<Account> result = testEntitySource().entityStream()
                .filter(account -> email.equals(account.email()))
                .findFirst();
        getByEmail.state(s -> s.namedEntityOrNull(result.orElse(null), "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public Page<Account> getLapsedRegistrations(Instant now, PageRequest pageRequest) {
        MethodObserver methodObserver = observer().forMethod("getLapsedRegistrations");
        methodObserver.arguments(i -> i
                .notNull(now, "now")
                .valueObject(pageRequest, "pageRequest"))
                .throwWhenInvalid();
        // Per account: does it hold a verification token at all, and is any of them still live?
        Map<AccountName, Boolean> anyLive = database.getNamed(EmailVerificationTokenTestEntitySource.class)
                .entityStream()
                .filter(token -> token.hasPurpose(TokenPurpose.VERIFY_EMAIL))
                .collect(Collectors.toMap(
                        EmailVerificationToken::account,
                        token -> !token.isExpired(now),
                        Boolean::logicalOr));
        List<Account> lapsed = testEntitySource().entityStream()
                .filter(account -> !account.emailVerified())
                .filter(account -> account.access() == AccessLevel.BROWSE_ONLY)
                .filter(account -> account.status() == AccountStatus.ACTIVE)
                .filter(account -> Boolean.FALSE.equals(anyLive.get(account.name())))
                .sorted(Comparator.comparing(account -> account.name().value()))
                .toList();
        Page<Account> page = pageOf(lapsed, pageRequest);
        methodObserver.state(s -> s.observableCollection(page.content(), "result")).observe(Level.WARN);
        return page;
    }

    @Override
    public Set<AccountName> deleteUnverified(Set<AccountName> names) {
        observer().arguments("deleteUnverified", i -> i.entityNameCollection(names, "names")).throwWhenInvalid();
        Set<AccountName> unverified = testEntitySource().getByEntityNameSet(names).stream()
                .filter(account -> !account.emailVerified())
                .map(Account::name)
                .collect(Collectors.toUnmodifiableSet());
        // Load the child source first: delete() enforces inbound foreign keys only from sources
        // already registered, and the token source loads lazily.
        database.getNamed(EmailVerificationTokenTestEntitySource.class);
        testEntitySource().delete(unverified);
        return unverified;
    }

    /** One page of an already-filtered, already-sorted list, honouring the request's lookahead. */
    private static Page<Account> pageOf(List<Account> sorted, PageRequest request) {
        int total = sorted.size();
        int pageSize = request.pageSize();
        int start = Math.min(request.offset(), total);
        int end = Math.min(start + pageSize, total);
        int remaining = total - end;
        int pagesAheadKnown = request.lookahead() == 0
                ? 0
                : Math.min((remaining + pageSize - 1) / pageSize, request.lookahead());
        boolean moreBeyondLookahead = remaining > request.lookahead() * pageSize;
        return new Page<>(sorted.subList(start, end), request.pageNumber(), pageSize,
                pagesAheadKnown, moreBeyondLookahead);
    }
}
