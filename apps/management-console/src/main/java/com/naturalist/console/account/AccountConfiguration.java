package com.naturalist.console.account;

import com.naturalist.account.AccountPolicy;
import com.naturalist.account.SecureTokens;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the two beans {@code accounts-core}'s {@code @DomainService} {@code
 * AccountCommandImpl} needs beyond its repositories and the shared {@code Clock} (already
 * provided as {@code usageClock} by {@link com.naturalist.console.usage.UsageConfiguration} and
 * resolved by type): a {@link SecureTokens} and the configured {@link AccountPolicy}. Those
 * adapters themselves and the {@code accounts-repository-rdbms} repositories are discovered
 * automatically by {@code DomainServiceScan} — deliberately not declared as {@code @Bean}s here.
 *
 * <p>No {@code Clock} bean is declared here — declaring a second one would collide with {@code
 * usageClock} and break by-type autowiring for every consumer.
 */
@Configuration
@EnableConfigurationProperties(AccountProperties.class)
class AccountConfiguration {

    @Bean
    SecureTokens secureTokens() {
        return SecureTokens.jdk();
    }

    @Bean
    AccountPolicy accountPolicy(AccountProperties properties) {
        return switch (properties.grantPolicy()) {
            case "self-serve" -> AccountPolicy.selfServe();
            case "request" -> AccountPolicy.request();
            default -> throw new IllegalStateException(
                    "naturalist.access.grant-policy must be 'self-serve' or 'request', was: "
                            + properties.grantPolicy());
        };
    }
}
