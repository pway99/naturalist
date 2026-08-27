package com.naturalist.usage;

/**
 * The percentage of a {@link UsageCounter}'s limit at which a
 * {@link AlertKind#WARNING} alert fires (see {@link UsagePolicy#warningThreshold}).
 *
 * <p>Exists solely so this scalar policy value can be supplied as a Spring
 * bean: {@code DomainServiceScan} registers {@code UsageQueryImpl} and
 * {@code UsageCommandImpl} as plain bean definitions with no explicit
 * constructor arguments, relying on Spring's implicit single-constructor
 * autowiring to resolve each parameter by type. That resolution is reliable
 * for a concrete reference type (as it was for the now-deleted
 * {@code UsageLimits}) but not for a bare {@code int}/{@code Integer}
 * constructor parameter, so the scalar is wrapped here rather than passed
 * raw. The app's {@code UsageConfiguration} supplies the single {@code
 * @Bean} instance, sourced from {@code UsageProperties.warningPercent()}.
 */
public record WarningPercent(int value) {
}
