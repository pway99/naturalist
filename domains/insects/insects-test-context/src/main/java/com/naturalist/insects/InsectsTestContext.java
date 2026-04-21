package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.extension.BeforeEachCallback;
// Should this be registered as an extension or exist with in a parent that is the extension
public class InsectsTestContext { //implements BeforeEachCallback {
    protected final NaturalistDatabase db;
    private final InsectRepository repository;
    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.ImageQuery imageQuery;
    private final InsectQuery insectQuery;

    private InsectsTestContext(NaturalistDatabase db) {
        this.db = db;
        this.repository = InsectRepository.create(new SpeciesRepositoryMock(db), new InsectImageRepositoryMock(db));
        this.speciesQuery = new SpeciesQueryImpl(repository.speciesRepository);
        this.imageQuery = new ImageQueryImpl(repository.imageRepository);
        this.insectQuery = new InsectQueryImpl(speciesQuery, imageQuery);
    }

    public static InsectsTestContext create(NaturalistDatabase db) {
        return new InsectsTestContext(db);
    }

    public InsectQuery insectQuery() {
        return insectQuery;
    }

}
