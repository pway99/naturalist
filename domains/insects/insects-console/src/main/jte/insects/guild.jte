@import com.naturalist.insects.InsectFunctionalRole
@import com.naturalist.insects.FunctionalGuild
@import com.naturalist.insects.console.AncestorIntro
@import com.naturalist.insects.console.BreadcrumbSegment
@import java.util.List

@param FunctionalGuild selectedGuild
@param FunctionalGuild[] guilds
@param List<InsectFunctionalRole> roles
@param List<BreadcrumbSegment> breadcrumb = List.of()
@param List<AncestorIntro> ancestorIntros = List.of()

@template.layout.page(title = "Guild: " + selectedGuild.name(), content = @`
    @template.insects.nav(active = "species", breadcrumb = breadcrumb)
    @template.insects.ancestorIntros(intros = ancestorIntros)

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

    <p>${roles.size()} catalog records in this guild, across family, genus, and species ranks.</p>

    <div class="entity-grid">
        @for(var r : roles)
            !{var parent = r.parentName();}
            !{var rankLabel = parent.rank().name();}
            <article>
                <header>
                    <strong>${parent.value()}</strong>
                    <span class="badge rank">${rankLabel}</span>
                    @if(r.beneficial())
                        <span class="badge beneficial">beneficial</span>
                    @endif
                </header>
                <div class="guilds">
                    @for(var guild : r.guilds())
                        <a href="/insects/guild/${guild.name()}"
                           class="badge guild-${guild.name().toLowerCase()}">${guild.name()}</a>
                    @endfor
                </div>
            </article>
        @endfor
    </div>
`)
