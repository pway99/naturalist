package com.naturalist.insects;

import com.naturalist.catalog.DomainId;
import com.naturalist.infrastructure.DomainService;

/**
 * The insects domain — see {@code domains/insects/}. Inverse references to
 * compounds (e.g. pipevine swallowtail sequestering aristolochic acid) compose
 * with the plants references on a chemistry detail page.
 * <p>
 * Owned by {@code insects-api} per the plan's "open {@link DomainId}"
 * decision: each domain ships its own slug-bearing subtype, the kernel knows
 * the names of no domains, and the catalog assembly enforces slug uniqueness
 * across registered subtypes at startup.
 */
@DomainService
public record InsectsDomain() implements DomainId {

    @Override
    public String value() {
        return "insects";
    }

    @Override
    public String toString() {
        return value();
    }
}
