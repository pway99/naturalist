package com.naturalist.persistence;

/** One foreign-key edge for {@link DboSchema}. `references` is e.g. "naturalist(id)". */
public @interface Fk {
    String columns();
    String references();
}
