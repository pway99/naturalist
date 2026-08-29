package com.naturalist.naturalist;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

@DboSchema(table = "naturalist_credential", primaryKey = "naturalist_id",
           foreignKeys = @Fk(columns = "naturalist_id", references = "naturalist(id)"),
           entity = NaturalistCredential.class)
final class NaturalistCredentialDbo implements Dbo {
    String name;          // the naturalist's slug: JOIN projection (read) / nested-select key (write)
    String passwordHash;

    static NaturalistCredentialDbo from(NaturalistCredential c) {
        NaturalistCredentialDbo d = new NaturalistCredentialDbo();
        d.name = c.name().value();
        d.passwordHash = c.passwordHash();
        Observer.forClass(NaturalistCredentialDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    NaturalistCredential toEntity() {
        return new NaturalistCredential(NaturalistName.of(name), passwordHash);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name")
                .notBlank(passwordHash, "passwordHash").maxLength(passwordHash, 80, "passwordHash");
    }
}
