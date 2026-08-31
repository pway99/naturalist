package com.naturalist.account;

/**
 * Thrown when a presented verification / reset token cannot be redeemed — unknown, expired,
 * already consumed, or of the wrong purpose. The cases are deliberately indistinguishable to
 * the caller so a probe cannot learn which tokens exist or have expired.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
