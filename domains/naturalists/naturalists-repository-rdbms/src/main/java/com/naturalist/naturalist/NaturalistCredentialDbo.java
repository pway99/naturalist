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
    Long naturalistId;   // resolved from naturalist.name at insert time
    String passwordHash;
    String name;          // read-only projection (aliased n.name); null on the write path

    static NaturalistCredentialDbo from(NaturalistCredential c, long naturalistId) {
        NaturalistCredentialDbo d = new NaturalistCredentialDbo();
        d.naturalistId = naturalistId;
        d.passwordHash = c.passwordHash();
        d.name = c.name().value();
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
                .notNull(naturalistId, "naturalistId")
                .notBlank(passwordHash, "passwordHash").maxLength(passwordHash, 80, "passwordHash");
    }
}
