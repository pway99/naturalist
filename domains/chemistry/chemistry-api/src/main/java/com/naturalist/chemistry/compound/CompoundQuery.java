package com.naturalist.chemistry.compound;

import com.naturalist.data.EntityQuery;
import com.naturalist.ddd.EntityNameSet;

public interface CompoundQuery extends EntityQuery<CompoundName, Compound, CompoundCollection> {

    EntityNameSet<CompoundName> allCompoundNames();
}
