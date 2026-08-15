package com.naturalist.garden;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

/**
 * Test data source for {@link CropType} — the crop types actually grown at Oak Vista, each tied to
 * the species the plants catalog already carries cultivars for. {@code lettuce} has a null plant:
 * it is grown and soil-tested for (the August 2026 FGL panel is a lettuce panel) without anyone
 * having decided which <em>Lactuca</em> it is, which is the normal state of affairs and the reason
 * the link is optional.
 * <p>
 * Backing catalog: {@code garden/crop-type.json}.
 */
public class CropTypeTestEntitySource extends TestEntitySource<CropTypeName, CropType> {

    public CropTypeTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("garden/crop-type.json");
    }
}
