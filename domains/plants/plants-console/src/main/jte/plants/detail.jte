@import com.naturalist.plants.Plant
@import com.naturalist.plants.cultivar.Cultivar
@import com.naturalist.plants.heritage.SeedLineage
@import com.naturalist.plants.management.PlantProgram
@import com.naturalist.plants.phytochemistry.PhytochemicalConstituent
@import java.util.List

@param Plant plant
@param List<Cultivar> cultivars = java.util.List.of()
@param List<PlantProgram> programs = java.util.List.of()
@param List<PhytochemicalConstituent> constituents = java.util.List.of()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = plant.name().value(), content = @`
    <a href="/plants" class="naturalist-backlink">&larr; Back to catalog</a>

    <h1>${plant.name().value()}</h1>
    <p>
        ${plant.taxonomy().order().value()} &middot; ${plant.taxonomy().family().value()}
        @if(plant.taxonomy().genus() != null)
            &middot; <em>${plant.taxonomy().genus().value()}</em>
        @endif
        @if(plant.taxonomy().species() != null)
            ${plant.taxonomy().species().value()}
        @endif
    </p>

    <section>
        <h2>Description</h2>
        <details open>
            <summary><strong>Preschool</strong></summary>
            $unsafe{descriptionPreschool.isEmpty() ? "<p>" + plant.description().preschool() + "</p>" : descriptionPreschool}
        </details>
        <details>
            <summary><strong>Elementary</strong></summary>
            $unsafe{descriptionElementary.isEmpty() ? "<p>" + plant.description().elementary() + "</p>" : descriptionElementary}
        </details>
        <details>
            <summary><strong>Secondary</strong></summary>
            $unsafe{descriptionSecondary.isEmpty() ? "<p>" + plant.description().secondary() + "</p>" : descriptionSecondary}
        </details>
        <details>
            <summary><strong>University</strong></summary>
            $unsafe{descriptionUniversity.isEmpty() ? "<p>" + plant.description().university() + "</p>" : descriptionUniversity}
        </details>
    </section>

    <section>
        <h2>Identity</h2>
        <ul>
            <li><strong>Slug:</strong> <code>${plant.name().value()}</code></li>
            <li><strong>Life form:</strong> ${plant.lifeForm().name()}</li>
            <li>
                <strong>Roles:</strong>
                @for(var role : plant.roles())
                    <span class="badge">${role.name()}</span>
                @endfor
            </li>
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
        <h2>Behavioral predicates</h2>
        <p class="description-preview">
            @if(plant.isKeystoneHost())
                <span class="badge">Keystone host</span>
            @endif
            @if(plant.supportsBiocontrolInsects())
                <span class="badge">Beneficial insect habitat</span>
            @endif
            @if(plant.isNitrogenFixer())
                <span class="badge">Nitrogen fixer</span>
            @endif
        </p>
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
                        &middot; ${c.fruitType().name()}
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
                        &middot; compound: <a href="/chemistry/${c.compoundName().value()}"><code>${c.compoundName().value()}</code></a>
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
