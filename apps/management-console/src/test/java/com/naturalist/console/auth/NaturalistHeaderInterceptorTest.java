package com.naturalist.console.auth;

import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalistHeaderInterceptorTest {

    private final NaturalistHeaderInterceptor interceptor = new NaturalistHeaderInterceptor();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publishesNaturalistSlug_forNaturalistPrincipal() {
        var principal = new NaturalistPrincipal(
                NaturalistName.of("patrick-way"), "Patrick", "{bcrypt}x");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities()));
        var request = new MockHttpServletRequest();

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(request.getAttribute(NaturalistHeaderInterceptor.CURRENT_NATURALIST_NAME))
                .isEqualTo("patrick-way");
    }

    @Test
    void publishesNull_forAnonymous() {
        var request = new MockHttpServletRequest();
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        assertThat(request.getAttribute(NaturalistHeaderInterceptor.CURRENT_NATURALIST_NAME)).isNull();
    }
}
