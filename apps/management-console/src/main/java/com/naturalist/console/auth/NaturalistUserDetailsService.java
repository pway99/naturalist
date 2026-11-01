package com.naturalist.console.auth;

import com.naturalist.account.Account;
import com.naturalist.account.AccessLevel;
import com.naturalist.account.AccountQuery;
import com.naturalist.account.AccountStatus;
import com.naturalist.console.admin.AdminProperties;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.Naturalist;
import com.naturalist.naturalist.NaturalistQuery;
import com.naturalist.observability.Level;
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
 * constraint observed at {@link Level#WARN}: {@code usernameWellFormed} (a non-email login),
 * {@code accountKnown} (email guessing), {@code naturalistLinked} (an orphaned account — a
 * data-integrity hole), {@code accountActive} (an attempt on a suspended account),
 * {@code isConfigAdmin}. Spring's {@code DaoAuthenticationProvider} hides "user not found"
 * from the caller as bad credentials; these metrics are where the distinction survives, so an
 * operator can tell email enumeration from password guessing. The downstream outcome of the
 * attempt itself (bad password, locked) is observed by {@link AuthenticationEventObserver}.
 *
 * <p>The admin password hash is computed once at construction — encoding it per attempt
 * handed one BCrypt round of CPU to anyone flooding {@code /login}.
 */
@Service
class NaturalistUserDetailsService implements UserDetailsService {

    private final Observer observer = Observer.forClass(getClass(), Observer.MonitoringMode.ALWAYS);

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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        boolean isConfigAdmin = admin.username().equals(username);
        if (isConfigAdmin) {
            observer.forMethod("loadUserByUsername")
                    .arguments(i -> i.isTrue(isConfigAdmin, "isConfigAdmin"))
                    .observe(Level.WARN);
            return User.withUsername(admin.username())
                    .password(adminPasswordHash)
                    .roles("ADMIN")
                    .build();
        }

        Optional<Account> account = Optional.empty();
        boolean wellFormed = true;
        try {
            account = accountQuery.getByEmail(username);
        } catch (InvariantViolationException malformedEmail) {
            wellFormed = false;   // a malformed login is "not found", not a 500
        }
        Optional<Naturalist> naturalist = account.flatMap(a -> naturalistQuery.byAccount(a.name()));

        final Optional<Account> found = account;
        final boolean usernameWellFormed = wellFormed;
        observer.forMethod("loadUserByUsername")
                .arguments(i -> i
                        .isTrue(usernameWellFormed, "usernameWellFormed")
                        .isTrue(found.isPresent(), "accountKnown")
                        .isTrue(naturalist.isPresent(), "naturalistLinked")
                        .isTrue(found.map(a -> a.status() == AccountStatus.ACTIVE).orElse(true), "accountActive"))
                .observe(Level.WARN);

        if (found.isEmpty() || naturalist.isEmpty()) {
            throw new UsernameNotFoundException(username);
        }
        Account resolved = found.get();
        return new NaturalistPrincipal(
                naturalist.get().name(),
                naturalist.get().givenName(),
                resolved.email(),
                resolved.passwordHash(),
                resolved.access() == AccessLevel.VISION,
                resolved.status() != AccountStatus.SUSPENDED);
    }
}
