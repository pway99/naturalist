package com.naturalist.naturalist;

import com.naturalist.account.AccountName;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

import org.jspecify.annotations.Nullable;

/**
 * Plain {@code @DomainService} write command (ADR-010 style; mirrors {@code AccountCommandImpl}).
 * {@link #create} mints a fresh, opaque {@link NaturalistName} and links the new ecological
 * record to an already-existing {@link AccountName} — this command never creates or validates
 * accounts itself, it only records the link. {@link #rename} changes only {@code publicHandle};
 * the opaque {@link NaturalistName} key is immutable and never crosses this command.
 */
@DomainService
class NaturalistCommandImpl implements NaturalistCommand {

    private final Observer observer = Observer.forClass(getClass());

    private final NaturalistRepository.NaturalistEntityRepository naturalists;

    NaturalistCommandImpl(NaturalistRepository.NaturalistEntityRepository naturalists) {
        observer.arguments("constructor", i -> i.notNull(naturalists, "naturalists"))
                .throwWhenInvalid();
        this.naturalists = naturalists;
    }

    @Override
    public NaturalistName create(AccountName account,
                                  String publicHandle,
                                  String givenName,
                                  @Nullable String familyName,
                                  NaturalistRole role,
                                  EcologicalStage stage,
                                  @Nullable String notes) {
        observer.arguments("create", i -> i
                        .entityName(account, "account")
                        .notBlank(publicHandle, "publicHandle")
                        .notBlank(givenName, "givenName")
                        .notNull(role, "role")
                        .notNull(stage, "stage"))
                .throwWhenInvalid();

        NaturalistName name = NaturalistName.create();
        naturalists.insert(new Naturalist(
                name, account, publicHandle, givenName, familyName, role, stage, notes));
        return name;
    }

    @Override
    public void rename(NaturalistName name, String newPublicHandle) {
        observer.arguments("rename", i -> i
                        .entityName(name, "name")
                        .notBlank(newPublicHandle, "newPublicHandle"))
                .throwWhenInvalid();

        Naturalist existing = naturalists.getByName(name).orElseThrow();
        naturalists.update(existing.withPublicHandle(newPublicHandle));
    }
}
