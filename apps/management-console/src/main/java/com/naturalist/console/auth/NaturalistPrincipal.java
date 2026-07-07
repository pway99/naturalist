package com.naturalist.console.auth;

import com.naturalist.naturalist.NaturalistName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Authenticated naturalist principal. Carries the {@link NaturalistName} so the
 * {@link CurrentNaturalist} seam can resolve identity without re-parsing a username.
 * Building the principal is the single point where a username becomes a NaturalistName;
 * every downstream reader sees the typed name (see design: seam discipline).
 */
public final class NaturalistPrincipal implements UserDetails {

    private static final Collection<GrantedAuthority> AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_NATURALIST"));

    private final NaturalistName naturalistName;
    private final String givenName;
    private final String passwordHash;

    public NaturalistPrincipal(NaturalistName naturalistName, String givenName, String passwordHash) {
        this.naturalistName = naturalistName;
        this.givenName = givenName;
        this.passwordHash = passwordHash;
    }

    public NaturalistName naturalistName() {
        return naturalistName;
    }

    public String givenName() {
        return givenName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTHORITIES;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return naturalistName.value();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
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
