package com.naturalist.insects;

import com.naturalist.data.NamedTestEntitySource;

public class InsectImageTestEntitySource extends NamedTestEntitySource<InsectImageId, InsectImage> {

    public InsectImageTestEntitySource() {
        loadFile("insects/insect-images.json");
    }
}
