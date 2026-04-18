package com.naturalist.insects;

import com.naturalist.data.TestEntitySource;

public class InsectImageTestEntitySource extends TestEntitySource<InsectImageId, InsectImageName, InsectImage> {

    public InsectImageTestEntitySource() {
        super(InsectImageId::of);
        loadFile("insects/insect-images.json");
    }
}
