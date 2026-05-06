package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class InsectImageTestEntitySource extends TestEntitySource<InsectImageId, InsectImage> {

    public InsectImageTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-images.json");
    }
}
