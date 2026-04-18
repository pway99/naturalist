package com.naturalist.ddd;

import com.naturalist.observability.Observable;
import org.jspecify.annotations.Nullable;

public interface Entity<ID extends PersistenceId<?>, NAME extends EntityName<?>> extends Observable {

    ID id();

    /**
     * The stable natural key for this entity. Never null.
     */
    NAME name();

    <E extends Entity<ID, NAME>> E withId(ID id);

}
