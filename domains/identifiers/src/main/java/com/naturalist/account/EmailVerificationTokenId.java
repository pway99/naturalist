package com.naturalist.account;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

/**
 * Surrogate UUIDv7 identity for an {@code EmailVerificationToken}. Auth-internal — never
 * crosses a domain boundary. Generated via the kernel generator ({@link #create()}); never
 * {@code UUID.randomUUID()}.
 */
public final class EmailVerificationTokenId extends EntityId {

    private EmailVerificationTokenId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static EmailVerificationTokenId of(UUID value) {
        return new EmailVerificationTokenId(value);
    }

    public static EmailVerificationTokenId create() {
        return new EmailVerificationTokenId(EntityId.newUUID());
    }
}
