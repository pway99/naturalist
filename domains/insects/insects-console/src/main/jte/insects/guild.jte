@import com.naturalist.insects.InsectSpecies
@import com.naturalist.insects.FunctionalGuild
@import java.util.List

@param FunctionalGuild selectedGuild
@param FunctionalGuild[] guilds
@param List<InsectSpecies> species

@template.layout.page(title = "Guild: " + selectedGuild.name(), content = @`
    <a href="/insects">&larr; Back to catalog</a>

    <h1>Functional Guild: ${selectedGuild.name()}</h1>

    <form>
        <label for="guild-select">Select guild</label>
        <select id="guild-select"
                onchange="window.location.href='/insects/guild/' + this.value">
            @for(var g : guilds)
                @if(g == selectedGuild)
                    <option value="${g.name()}" selected>${g.name()}</option>
                @else
                    <option value="${g.name()}">${g.name()}</option>
                @endif
            @endfor
        </select>
    </form>

    <p>${species.size()} species in this guild.</p>

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
                        <a href="/insects/guild/${guild.name()}"
                           class="badge guild-${guild.name().toLowerCase()}">${guild.name()}</a>
                    @endfor
                </div>
                <p class="description-preview">${s.description().preschool()}</p>
            </article>
        @endfor
    </div>
`)
