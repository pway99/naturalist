package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class InsectImageQueryImpl
        extends AbstractEntityQuery<
        InsectImageId,
        InsectImage,
        ImageCollection,
        InsectRepository.ImageRepository>
        implements InsectQuery.ImageQuery {

    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.GenusQuery genusQuery;
    private final InsectQuery.FamilyQuery familyQuery;

    InsectImageQueryImpl(InsectRepository.ImageRepository repository,
                   InsectQuery.SpeciesQuery speciesQuery,
                   InsectQuery.GenusQuery genusQuery,
                   InsectQuery.FamilyQuery familyQuery) {
        super(repository);
        this.speciesQuery = speciesQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
    }

    @Override
    public ImageCollection findByNameSet(Set<InsectImageId> names) {
        observer().arguments("findByNameSet", i -> i
                        .identifierSet(names, "names"))
                .throwWhenInvalid();
        return ImageCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public ImageCollection forParentName(InsectRankName parentName) {
        observer().arguments("forParentName", i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        return ImageCollection.of(repository().getByParentName(parentName));
    }

    @Override
    public ImageCollection forRankHierarchy(InsectRankName rankName) {
        observer().arguments("forRankHierarchy", i -> i.identifier(rankName, "rankName"))
                .throwWhenInvalid();
        var images = new ArrayList<>(forParentName(rankName).stream().toList());
        switch (rankName) {
            case InsectSpeciesName _ -> { /* species is the leaf — no descendants */ }
            case InsectGenusName genusName -> collectForGenus(images, genusName);
            case InsectFamilyName familyName -> collectForFamily(images, familyName);
            case InsectOrderName orderName -> collectForOrder(images, orderName);
            case InsectSubspeciesName _ -> { /* no entity yet */ }
        }
        return ImageCollection.of(images);
    }

    private void collectForGenus(List<InsectImage> images, InsectGenusName genusName) {
        for (var species : speciesQuery.forGenusName(genusName).stream().toList()) {
            images.addAll(forParentName(species.name()).stream().toList());
        }
    }

    private void collectForFamily(List<InsectImage> images, InsectFamilyName familyName) {
        for (var genus : genusQuery.forFamilyName(familyName).stream().toList()) {
            images.addAll(forParentName(genus.name()).stream().toList());
            collectForGenus(images, genus.name());
        }
    }

    private void collectForOrder(List<InsectImage> images, InsectOrderName orderName) {
        for (var family : familyQuery.forOrderName(orderName).stream().toList()) {
            images.addAll(forParentName(family.name()).stream().toList());
            collectForFamily(images, family.name());
        }
    }
}