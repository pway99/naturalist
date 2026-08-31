package com.naturalist.account;

/**
 * Deployment policy for how vision access is granted, injected into {@link AccountCommand} so
 * one command serves either mode. Self-serve grants VISION the moment email is verified; the
 * request process withholds it until an admin grants it.
 */
public interface AccountPolicy {

    boolean grantsVisionOnVerification();

    /** Verifying email also grants VISION. */
    static AccountPolicy selfServe() {
        return () -> true;
    }

    /** Verifying email leaves the account BROWSE_ONLY until an admin grants VISION. */
    static AccountPolicy request() {
        return () -> false;
    }
}
