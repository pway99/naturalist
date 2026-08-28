package com.naturalist.naturalist;

import com.naturalist.observability.Constraints;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

@DboSchema(table = "naturalist_credential", primaryKey = "naturalist_id",
           foreignKeys = @Fk(columns = "naturalist_id", references = "naturalist(id)"))
final class NaturalistCredentialDbo implements Dbo {
    Long naturalist_id;   // resolved from naturalist.name at insert time
    String password_hash;
    String name;          // read-only projection (aliased n.name); null on the write path

    static NaturalistCredentialDbo from(NaturalistCredential c, long naturalistId) {
        NaturalistCredentialDbo d = new NaturalistCredentialDbo();
        d.naturalist_id = naturalistId;
        d.password_hash = c.passwordHash();
        d.name = c.name().value();
        return d;
    }

    NaturalistCredential toEntity() {
        return new NaturalistCredential(NaturalistName.of(name), password_hash);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(naturalist_id, "naturalist_id")
                .notBlank(password_hash, "password_hash").maxLength(password_hash, 80, "password_hash");
    }
}
