package com.naturalist.plants.spring;

import com.naturalist.infrastructure.DomainService;

/**
 * Test fixture placed under {@code com.naturalist.plants} so the
 * {@link com.naturalist.spring.DomainServiceScan DomainServiceScan}'s
 * pilot base packages discover it without dragging in any production
 * plants module.
 */
@DomainService
public class MarkedService {
}
