@import com.naturalist.plants.PlantEcologicalRole
@import com.naturalist.plants.PlantSpecies
@import com.naturalist.plants.cultivar.Cultivar
@import com.naturalist.plants.heritage.SeedLineage
@import com.naturalist.plants.management.PlantProgram
@import com.naturalist.plants.phytochemistry.PhytochemicalConstituent
@import java.util.List
@import com.naturalist.plants.console.BreadcrumbSegment
@import com.naturalist.clades.Clade

@param PlantSpecies plant
@param PlantEcologicalRole ecologicalRole = null
@param List<Cultivar> cultivars = java.util.List.of()
@param List<PlantProgram> programs = java.util.List.of()
@param List<PhytochemicalConstituent> constituents = java.util.List.of()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""
@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()
@param List<Clade> cladeTrail = java.util.List.of()

@template.layout.page(title = plant.name().value(), content = @`
    @template.plants.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)
    <h1>${plant.name().value()}</h1>
    <p>
        <a href="/plants/genera/${plant.genusName().value()}">${plant.genusName().value()}</a>
        &middot; <em>${plant.epithet().value()}</em>
    </p>

    @template.components.description(
    preschool = descriptionPreschool,
    elementary = descriptionElementary,
    secondary = descriptionSecondary,
    university = descriptionUniversity)

    <section>
        <h2>Identity</h2>
        <ul>
            <li><strong>Slug:</strong> <code>${plant.name().value()}</code></li>
            <li><strong>Growth habit:</strong> ${plant.growthHabit().name()}</li>
            <li><strong>Life cycle:</strong> ${plant.lifeCycle().name()}</li>
            @if(!plant.nativeBioregions().isEmpty())
                <li>
                    <strong>Native bioregions:</strong>
                    @for(var region : plant.nativeBioregions())
                        <span class="badge">${region.getClass().getSimpleName()}</span>
                    @endfor
                </li>
            @endif
        </ul>
    </section>

    <section>
        <h2>Ecological roles</h2>
        @if(ecologicalRole == null)
            <p class="not-reported">This plant's ecology has not been characterised yet.</p>
        @else
            <p class="description-preview">
                @for(var role : ecologicalRole.roles())
                    <span class="badge">${role.name()}</span>
                @endfor
            </p>
            <p class="description-preview">
                @if(ecologicalRole.isKeystoneHost())
                    <span class="badge">Keystone host</span>
                @endif
                @if(ecologicalRole.supportsBiocontrolInsects())
                    <span class="badge">Beneficial insect habitat</span>
                @endif
                @if(ecologicalRole.isNitrogenFixer())
                    <span class="badge">Nitrogen fixer</span>
                @endif
            </p>
        @endif
    </section>

    @if(!cultivars.isEmpty())
        <section>
            <h2>Cultivars (${cultivars.size()})</h2>
            <ul>
                @for(var c : cultivars)
                    <li>
                        <a href="/plants/cultivars/${c.name().value()}"><strong>${c.commonName()}</strong></a>
                        &middot; <code>${c.name().value()}</code>
                        &middot; ${c.varietyType().name()}
                        @if(c.fruitType() != null)
                            &middot; ${c.fruitType().name()}
                        @endif
                        &middot; ${c.seedSavingPolicy().name()}
                    </li>
                @endfor
            </ul>
        </section>
    @endif

    @if(!programs.isEmpty())
        <section>
            <h2>Plant programs (${programs.size()})</h2>
            <ul>
                @for(var p : programs)
                    <li>
                        <a href="/plants/programs/${p.name().value()}"><strong>${p.name().value()}</strong></a>
                        @if(p.hasConstraint())
                            <span class="badge guild-apex_predator">constraint</span>
                        @endif
                        <br><small>${p.description().preschool()}</small>
                    </li>
                @endfor
            </ul>
        </section>
    @endif

    @if(!constituents.isEmpty())
        <section>
            <h2>Phytochemistry (${constituents.size()})</h2>
            <p class="description-preview">
                Compounds this plant produces, the role(s) each plays in the plant's
                biology, the tissues where they accumulate, and the induction mode.
            </p>
            <ul>
                @for(var c : constituents)
                    <li>
                        <a href="/plants/phytochemistry/${c.name().value()}"><strong>${c.name().value()}</strong></a>
                        &middot; compound: <a
                                href="/chemistry/${c.compoundName().value()}"><code>${c.compoundName().value()}</code></a>
                        &middot; <span class="badge">${c.category().name()}</span>
                        &middot; ${c.induction().name()}
                        <br><small>
                            roles:
                            @for(var role : c.roles())
                                <span class="badge">${role.getClass().getSimpleName()}</span>
                            @endfor
                            &middot; tissues:
                            @for(var tissue : c.tissues())
                                <span class="badge">${tissue.name()}</span>
                            @endfor
                        </small>
                    </li>
                @endfor
            </ul>
        </section>
    @endif
`)
