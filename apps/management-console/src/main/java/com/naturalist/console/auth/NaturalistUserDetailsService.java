package com.naturalist.console.auth;

import com.naturalist.account.Account;
import com.naturalist.account.AccessLevel;
import com.naturalist.account.AccountQuery;
import com.naturalist.account.AccountStatus;
import com.naturalist.console.admin.AdminProperties;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.NaturalistQuery;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authenticates the config admin first (unchanged {@link AdminProperties} path,
 * {@code ROLE_ADMIN}); otherwise resolves an {@link Account} by login email and
 * builds a {@link NaturalistPrincipal} ({@code ROLE_NATURALIST}). This class
 * and {@link CurrentNaturalist}/{@link CurrentNaturalistView} are the only places that
 * turn a username into a naturalist identity (design: seam discipline).
 */
@Service
class NaturalistUserDetailsService implements UserDetailsService {

    private final AdminProperties admin;
    private final PasswordEncoder passwordEncoder;
    private final NaturalistQuery naturalistQuery;
    private final AccountQuery accountQuery;

    NaturalistUserDetailsService(AdminProperties admin,
                                 PasswordEncoder passwordEncoder,
                                 NaturalistQuery naturalistQuery,
                                 AccountQuery accountQuery) {
        this.admin = admin;
        this.passwordEncoder = passwordEncoder;
        this.naturalistQuery = naturalistQuery;
        this.accountQuery = accountQuery;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (admin.username().equals(username)) {
            return User.withUsername(admin.username())
                    .password(passwordEncoder.encode(admin.password()))
                    .roles("ADMIN")
                    .build();
        }
        Account account;
        try {
            account = accountQuery.getByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException(username));
        } catch (InvariantViolationException malformedEmail) {
            throw new UsernameNotFoundException(username);   // a malformed login is "not found", not a 500
        }
        var naturalist = naturalistQuery.byAccount(account.name())
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return new NaturalistPrincipal(
                naturalist.name(),
                naturalist.givenName(),
                account.email(),
                account.passwordHash(),
                account.access() == AccessLevel.VISION,
                account.status() != AccountStatus.SUSPENDED);
    }
}
