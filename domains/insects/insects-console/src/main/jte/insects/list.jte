@import com.naturalist.insects.InsectSpecies
@import com.naturalist.insects.FunctionalGuild
@import java.util.List

@param List<InsectSpecies> species

@template.layout.page(title = "Insect Catalog", content = @`
    <h1>Insect Catalog</h1>
    <p>${species.size()} species documented at Oak Vista.</p>

    <div class="species-grid">
        @for(var s : species)
            <article>
                <header>
                    <a href="/insects/${s.name().value()}">
                        <strong>${s.name().value()}</strong>
                    </a>
                    @if(s.beneficial())
                        <span class="badge beneficial">beneficial</span>
                    @endif
                </header>
                <p class="taxonomy">
                    ${s.taxonomy().order().value()} &middot; ${s.taxonomy().family().value()}
                    @if(s.taxonomy().genus() != null)
                        &middot; <em>${s.taxonomy().genus().value()}</em>
                    @endif
                </p>
                <div class="guilds">
                    @for(var guild : s.guilds())
                        <span class="badge guild-${guild.name().toLowerCase()}">${guild.name()}</span>
                    @endfor
                </div>
                <p class="description-preview">${s.description().preschool()}</p>
            </article>
        @endfor
    </div>
`)
