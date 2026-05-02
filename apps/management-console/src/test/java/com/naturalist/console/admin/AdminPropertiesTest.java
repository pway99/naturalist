package com.naturalist.console.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Confirms the admin credential pair has no defaults — a missing or
 * blank {@code naturalist.admin.username} or
 * {@code naturalist.admin.password} surfaces as a constructor failure
 * during {@link org.springframework.boot.context.properties.ConfigurationProperties}
 * binding, which in turn fails Spring context startup.
 */
class AdminPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EnableAdminProperties.class);

    @Test
    void rejectsBlankUsername() {
        assertThatThrownBy(() -> new AdminProperties("", "p"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }

    @Test
    void rejectsNullUsername() {
        assertThatThrownBy(() -> new AdminProperties(null, "p"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }

    @Test
    void rejectsBlankPassword() {
        assertThatThrownBy(() -> new AdminProperties("u", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");
    }

    @Test
    void rejectsNullPassword() {
        assertThatThrownBy(() -> new AdminProperties("u", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");
    }

    @Test
    void acceptsValidCredentials() {
        var props = new AdminProperties("u", "p");
        assertThat(props.username()).isEqualTo("u");
        assertThat(props.password()).isEqualTo("p");
    }

    @Test
    void contextFailsWhenUsernameBlank() {
        contextRunner
                .withPropertyValues(
                        "naturalist.admin.username=",
                        "naturalist.admin.password=p")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("username"));
    }

    @Test
    void contextFailsWhenPasswordMissing() {
        contextRunner
                .withPropertyValues("naturalist.admin.username=u")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("password"));
    }

    @Test
    void contextStartsWhenBothPresent() {
        contextRunner
                .withPropertyValues(
                        "naturalist.admin.username=u",
                        "naturalist.admin.password=p")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AdminProperties.class).username()).isEqualTo("u");
                });
    }

    @Configuration
    @EnableConfigurationProperties(AdminProperties.class)
    static class EnableAdminProperties {
    }
}
