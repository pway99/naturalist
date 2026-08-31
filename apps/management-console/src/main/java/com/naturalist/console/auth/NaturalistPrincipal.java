package com.naturalist.console.auth;

import com.naturalist.naturalist.NaturalistName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Authenticated naturalist principal. Carries the {@link NaturalistName} so the
 * {@link CurrentNaturalist} seam can resolve identity without re-parsing a username.
 * Building the principal is the single point where an account becomes a NaturalistName;
 * every downstream reader sees the typed name (see design: seam discipline).
 * <p>
 * Login username is the account {@code email}. {@code VISION} is carried as a plain
 * granted authority (not a role) alongside {@code ROLE_NATURALIST}, present only when
 * the backing account's access level is {@code VISION}.
 */
public final class NaturalistPrincipal implements UserDetails {

    private final NaturalistName naturalistName;
    private final String givenName;
    private final String email;
    private final String passwordHash;
    private final boolean accountNonLocked;
    private final Collection<GrantedAuthority> authorities;

    public NaturalistPrincipal(NaturalistName naturalistName, String givenName, String email,
                               String passwordHash, boolean hasVision, boolean accountNonLocked) {
        this.naturalistName = naturalistName;
        this.givenName = givenName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.accountNonLocked = accountNonLocked;
        var auths = new ArrayList<GrantedAuthority>();
        auths.add(new SimpleGrantedAuthority("ROLE_NATURALIST"));
        if (hasVision) auths.add(new SimpleGrantedAuthority("VISION"));
        this.authorities = List.copyOf(auths);
    }

    public NaturalistName naturalistName() {
        return naturalistName;
    }

    public String givenName() {
        return givenName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
