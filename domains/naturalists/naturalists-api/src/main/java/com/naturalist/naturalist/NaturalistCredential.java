package com.naturalist.naturalist;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Authentication credential for a {@link Naturalist}, keyed 1:1 by the same
 * {@link NaturalistName}. Deliberately separate from the {@code Naturalist}
 * ecological record: a naturalist is an actor, not an account. {@code passwordHash}
 * is an encoded (bcrypt, {@code {bcrypt}$2a$…}) hash produced by the app's
 * {@code PasswordEncoder} — never a plaintext password.
 */
public record NaturalistCredential(
        NaturalistName name,
        String passwordHash
) implements NamedEntity<NaturalistName> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .notBlank(passwordHash, "passwordHash");
    }
}
