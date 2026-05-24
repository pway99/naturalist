@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.InsectSpecies
@import java.util.List

@param InsectGenus genus
@param List<InsectSpecies> species
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = genus.genus().value(), content = @`
    @template.insects.nav(active = "genus")
    <h1><em>${genus.genus().value()}</em></h1>
    <p class="taxonomy">
        ${genus.order().value()} &middot;
        <a href="/insects/families/${genus.familyName().value()}">${genus.family().value()}</a>
    </p>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    <section>
        <h2>Species in this genus</h2>
        @if(species.isEmpty())
            <p><em>No species catalogued under this genus yet.</em></p>
        @else
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
                            <dt>species</dt>
                            <dd><em>${s.epithet().value()}</em></dd>
                        </dl>
                    </article>
                @endfor
            </div>
        @endif
    </section>
`)
