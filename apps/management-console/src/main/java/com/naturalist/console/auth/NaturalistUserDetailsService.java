package com.naturalist.console.auth;

import com.naturalist.console.admin.AdminProperties;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.naturalist.NaturalistCredentialQuery;
import com.naturalist.naturalist.NaturalistQuery;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authenticates the config admin first (unchanged {@link AdminProperties} path,
 * {@code ROLE_ADMIN}); otherwise resolves a {@link com.naturalist.naturalist.NaturalistCredential}
 * by slug and builds a {@link NaturalistPrincipal} ({@code ROLE_NATURALIST}). This class
 * and {@link CurrentNaturalist}/{@link CurrentNaturalistView} are the only places that
 * turn a username into a naturalist identity (design: seam discipline).
 */
@Service
class NaturalistUserDetailsService implements UserDetailsService {

    private final AdminProperties admin;
    private final PasswordEncoder passwordEncoder;
    private final NaturalistQuery naturalistQuery;
    private final NaturalistCredentialQuery credentialQuery;

    NaturalistUserDetailsService(AdminProperties admin,
                                 PasswordEncoder passwordEncoder,
                                 NaturalistQuery naturalistQuery,
                                 NaturalistCredentialQuery credentialQuery) {
        this.admin = admin;
        this.passwordEncoder = passwordEncoder;
        this.naturalistQuery = naturalistQuery;
        this.credentialQuery = credentialQuery;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (admin.username().equals(username)) {
            return User.withUsername(admin.username())
                    .password(passwordEncoder.encode(admin.password()))
                    .roles("ADMIN")
                    .build();
        }

        NaturalistName name = NaturalistName.of(username);
        if (name.isNotValid()) {
            throw new UsernameNotFoundException(username);
        }

        var credential = credentialQuery.getByName(name)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        var naturalist = naturalistQuery.getByName(name)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        return new NaturalistPrincipal(name, naturalist.givenName(), credential.passwordHash());
    }
}
