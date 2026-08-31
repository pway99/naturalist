package com.naturalist.account;

/**
 * Why an {@link EmailVerificationToken} was minted — proving control of an email address at
 * registration, or authorising a password reset. One token type serves both flows.
 */
public enum TokenPurpose {
    VERIFY_EMAIL,
    RESET_PASSWORD
}
