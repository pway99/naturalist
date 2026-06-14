package com.naturalist.clades;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Read-side enumeration of the sealed {@link Clade} permit set, for building a
 * browsable tree of life. {@link CladeTraversal} walks <em>up</em> the parent
 * chain; this catalog provides the whole permit set and the <em>down</em>
 * (children) direction the kernel otherwise lacks.
 *
 * <p>{@link #all()} reflects over {@code Clade.class.getPermittedSubclasses()}
 * — every permit is a stateless no-arg record — so the catalog tracks the
 * sealed permit list automatically with no second list to maintain. New permits
 * appear here at runtime with no change to this file.
 */
public final class CladeCatalog {

    private CladeCatalog() {
    }

    /** Every clade permit, in no guaranteed order. */
    public static List<Clade> all() {
        return Arrays.stream(Clade.class.getPermittedSubclasses())
                .map(CladeCatalog::instantiate)
                .toList();
    }

    /** Direct descendants of {@code parent}, sorted by display name. */
    public static List<Clade> childrenOf(Clade parent) {
        return all().stream()
                .filter(c -> c.parent().map(p -> p.equals(parent)).orElse(false))
                .sorted(Comparator.comparing(Clade::displayName))
                .toList();
    }

    private static Clade instantiate(Class<?> permit) {
        try {
            return (Clade) permit.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate clade permit: " + permit, e);
        }
    }
}
