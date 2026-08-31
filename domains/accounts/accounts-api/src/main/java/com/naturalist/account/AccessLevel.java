package com.naturalist.account;

/**
 * The vision-identification entitlement of an {@link Account}.
 *
 * <p>{@code BROWSE_ONLY} is the state of a freshly registered account: it may sign
 * in and browse, but the {@code VISION} authority is withheld. {@code VISION} is
 * granted by email verification (self-serve policy) or by an admin (request policy);
 * that grant flips this field. See the accounts auth redesign design doc.
 */
public enum AccessLevel {
    BROWSE_ONLY,
    VISION
}
