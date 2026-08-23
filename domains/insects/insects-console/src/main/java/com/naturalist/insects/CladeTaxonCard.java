package com.naturalist.insects;

/**
 * A taxon placed at a clade, as shown on the insect clade page: a display name and the URL
 * of its catalog detail page. Insects place clades at every rank, so the clade page groups
 * these into Order / Family / Genus / Species sections — which is how a naturalist discovers
 * that insect clades extend below Order, unlike the supra-ordinal plant clades.
 */
public record CladeTaxonCard(String url, String name) {
}
