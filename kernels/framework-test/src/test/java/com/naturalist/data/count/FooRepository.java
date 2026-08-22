package com.naturalist.data.count;

import java.util.List;

interface FooRepository {
    String getByName(String name);
    List<String> getByEntityNameSet(java.util.Set<String> names);
}
