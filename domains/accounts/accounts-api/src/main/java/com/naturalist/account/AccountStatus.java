package com.naturalist.account;

/**
 * Lifecycle state of an {@link Account}. {@code SUSPENDED} maps to Spring Security's
 * {@code accountNonLocked = false} at the console boundary — the abuse ban switch.
 */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED
}
