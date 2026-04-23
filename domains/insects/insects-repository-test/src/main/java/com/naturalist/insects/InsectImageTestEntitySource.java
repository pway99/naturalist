package com.naturalist.insects;

import com.naturalist.data.TestEntitySource;

public class InsectImageTestEntitySource extends TestEntitySource<InsectImageId, InsectImage> {

    public InsectImageTestEntitySource() {
        loadFile("insects/insect-images.json");
    }
}
