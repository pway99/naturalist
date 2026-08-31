package com.naturalist.account;

/**
 * Thrown by {@link AccountCommand#register} when the email is already registered.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
