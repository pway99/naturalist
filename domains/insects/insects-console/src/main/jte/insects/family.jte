@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.InsectSpecies
@import java.util.List

@param InsectFamily family
@param List<InsectGenus> genera
@param List<InsectSpecies> species
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = family.family().value(), content = @`
    @template.insects.nav(active = "family")
    <h1>${family.family().value()}</h1>
    <p class="taxonomy">${family.order().value()}</p>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    <section>
        <h2>Genera in this family</h2>
        @if(genera.isEmpty())
            <p><em>No genera catalogued under this family yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var g : genera)
                    !{var commonName = g.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(g.name().value());}
                    <article>
                        <header>
                            <a href="/insects/genera/${g.name().value()}">
                                <strong>${commonName}</strong>
                            </a>
                        </header>
                        <dl class="taxonomy">
                            <dt>genus</dt>
                            <dd><em>${g.genus().value()}</em></dd>
                        </dl>
                    </article>
                @endfor
            </div>
        @endif
    </section>

    @if(!species.isEmpty())
        <section>
            <h2>Species attached at family rank</h2>
            <p><em>Identified to family — no catalogued genus yet.</em></p>
            <div class="entity-grid">
                @for(var s : species)
                    !{var commonName = s.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(s.name().value());}
                    <article>
                        <header>
                            <a href="/insects/${s.name().value()}">
                                <strong>${commonName}</strong>
                            </a>
                        </header>
                        <dl class="taxonomy">
                            @if(s.taxonomy().species() != null)
                                <dt>species</dt>
                                <dd><em>${s.taxonomy().species().value()}</em></dd>
                            @endif
                        </dl>
                    </article>
                @endfor
            </div>
        </section>
    @endif
`)
