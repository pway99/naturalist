package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.data.EntityRepository;

interface CitationRepository extends EntityRepository<CitationName, Citation> {
}
