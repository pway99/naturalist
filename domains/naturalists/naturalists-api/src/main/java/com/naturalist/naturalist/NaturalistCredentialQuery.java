package com.naturalist.naturalist;

import com.naturalist.data.EntityQuery;
import com.naturalist.naturalist.NaturalistEntityCollections.NaturalistCredentialCollection;

/**
 * Read port for {@link NaturalistCredential} entities. The console's
 * {@code UserDetailsService} is its only caller — {@code getByName} resolves a
 * credential by the naturalist's slug.
 */
public interface NaturalistCredentialQuery
        extends EntityQuery<NaturalistName, NaturalistCredential, NaturalistCredentialCollection> {
}
