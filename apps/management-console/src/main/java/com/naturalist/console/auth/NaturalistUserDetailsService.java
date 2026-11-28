package com.naturalist.console.auth;

import com.naturalist.account.Account;
import com.naturalist.account.AccessLevel;
import com.naturalist.account.AccountQuery;
import com.naturalist.account.AccountStatus;
import com.naturalist.console.admin.AdminProperties;
import com.naturalist.ddd.EmailAddress;
import com.naturalist.naturalist.Naturalist;
import com.naturalist.naturalist.NaturalistQuery;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Authenticates the config admin first (unchanged {@link AdminProperties} path,
 * {@code ROLE_ADMIN}); otherwise resolves an {@link Account} by login email and
 * builds a {@link NaturalistPrincipal} ({@code ROLE_NATURALIST}). This class
 * and {@link CurrentNaturalist}/{@link CurrentNaturalistView} are the only places that
 * turn a username into a naturalist identity (design: seam discipline).
 *
 * <p><strong>Observability.</strong> Login is the attack surface, so the {@link Observer} runs
 * in {@link Observer.MonitoringMode#ALWAYS} and every resolution decision is a predicate-named
 * constraint observed at {@link Level#WARN}: {@code username} (a non-email login, refused as
 * not found), {@code accountKnown} (email guessing), and only once the account is known
 * {@code account} (a stored account that fails its own invariants), {@code naturalistLinked}
 * (an orphaned account — a data-integrity hole) and {@code accountActive} (an attempt on a
 * suspended account), so each case is counted under one name; and {@code isConfigAdmin}.
 * A login without {@code accountKnown} or {@code naturalistLinked} is refused as not found.
 * Spring's {@code DaoAuthenticationProvider} hides "user not found"
 * from the caller as bad credentials; these metrics are where the distinction survives, so an
 * operator can tell email enumeration from password guessing. The downstream outcome of the
 * attempt itself (bad password, locked) is observed by {@link AuthenticationEventObserver}.
 *
 * <p>The admin password hash is computed once at construction — encoding it per attempt
 * handed one BCrypt round of CPU to anyone flooding {@code /login}.
 */
@Service
class NaturalistUserDetailsService implements UserDetailsService {

    private static final Observer observer =
            Observer.forClass(NaturalistUserDetailsService.class, Observer.MonitoringMode.ALWAYS);

    private final AdminProperties admin;
    private final String adminPasswordHash;
    private final NaturalistQuery naturalistQuery;
    private final AccountQuery accountQuery;

    NaturalistUserDetailsService(AdminProperties admin,
                                 PasswordEncoder passwordEncoder,
                                 NaturalistQuery naturalistQuery,
                                 AccountQuery accountQuery) {
        this.admin = admin;
        this.adminPasswordHash = passwordEncoder.encode(admin.password());
        this.naturalistQuery = naturalistQuery;
        this.accountQuery = accountQuery;
    }

    //TODO: We need to revisit userName since an email is rqueired, I bleieve there is a work item for this in the todo docs
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MethodObserver methodObserver = observer.forMethod("loadUserByUsername");
        methodObserver.arguments(c -> c
                .notNull(username, "username"))
                .throwWhenInvalid();
        boolean isConfigAdmin = admin.username().equals(username);
        if (isConfigAdmin) {
            methodObserver
                    .state(i -> i.isTrue(isConfigAdmin, "isConfigAdmin"))
                    .observe(Level.WARN);
            return User.withUsername(admin.username())
                    .password(adminPasswordHash)
                    .roles("ADMIN")
                    .build();
        }

        // Canonical form — registration stored the address through the same EmailAddress.of.
        EmailAddress email = EmailAddress.of(username);
        // A malformed login is "not found", not a 500: Spring Security takes the answer as an exception.
        methodObserver
                .state(i -> i.emailAddress(email, "username"))
                .throwWhenInvalid(Level.WARN, () -> new UsernameNotFoundException(username));
        Optional<Account> found = accountQuery.getByEmail(email.value());
        Optional<Naturalist> naturalist = found.flatMap(a -> naturalistQuery.byAccount(a.name()));

        // One name per case: the account's own checks run only when there is an account.
        methodObserver
                .state(i -> i
                        .isPresent(found, "accountKnown")
                        .when(found.isPresent(), c -> c
                                .namedEntity(found.get(), "account")
                                .isPresent(naturalist, "naturalistLinked")
                                .namedEntityOrNull(naturalist.orElse(null), "naturalist")
                                .isTrue(found.get().isActive(), "accountActive")))
                .observe(Level.WARN);

        if (!resolvesToNaturalist(found, naturalist)) {
            throw new UsernameNotFoundException(username);
        }
        Account resolved = found.get();
        return new NaturalistPrincipal(
                naturalist.get().name(),
                naturalist.get().givenName(),
                resolved.email(),
                resolved.passwordHash(),
                resolved.access() == AccessLevel.VISION,
                resolved.emailVerified(),
                resolved.status() != AccountStatus.SUSPENDED);
    }

    /** A login resolves to a principal only through a known account linked to a naturalist. */
    private static boolean resolvesToNaturalist(Optional<Account> found, Optional<Naturalist> naturalist) {
        return found.isPresent() && naturalist.isPresent();
    }
}
