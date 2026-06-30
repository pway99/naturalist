@import com.naturalist.library.CladeStep
@import com.naturalist.library.CladeView

@param CladeView view
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = view.subject().displayName(), content = @`
    <nav class="dual-breadcrumb" aria-label="Clade navigation">
        <%-- Row labels --%>
        <span class="dual-breadcrumb-clade"><a href="/concepts/clade">Phylogenetic lineage</a></span>
        <span class="dual-breadcrumb-rank"><a href="/concepts/taxonomic-rank">Linnaean rank ladder</a></span>

        @for(var step : view.ancestry())
            <%-- Separator column --%>
            <span class="dual-breadcrumb-clade dual-breadcrumb-sep" aria-hidden="true">›</span>
            <span class="dual-breadcrumb-rank dual-breadcrumb-spacer"></span>

            <%-- Data column --%>
            <span class="dual-breadcrumb-clade"><a href="/clades/${step.cladeSlug()}">${step.displayName()}</a></span>
            @if(step.rank().isPresent())
                !{var r = step.rank().get().name();}
                <span class="dual-breadcrumb-rank"><a href="/concepts/${r.toLowerCase()}">${r.substring(0, 1)}${r.substring(1).toLowerCase()}</a></span>
            @else
                <span class="dual-breadcrumb-rank dual-breadcrumb-gap" aria-label="no rank">—</span>
            @endif
        @endfor

        <%-- Subject separator --%>
        <span class="dual-breadcrumb-clade dual-breadcrumb-sep" aria-hidden="true">›</span>
        <span class="dual-breadcrumb-rank dual-breadcrumb-spacer"></span>

        <%-- Subject column --%>
        <span class="dual-breadcrumb-clade dual-breadcrumb-current"><strong>${view.subject().displayName()}</strong></span>
        @if(view.subject().rank().isPresent())
            !{var sr = view.subject().rank().get().name();}
            <span class="dual-breadcrumb-rank"><a href="/concepts/${sr.toLowerCase()}">${sr.substring(0, 1)}${sr.substring(1).toLowerCase()}</a></span>
        @else
            <span class="dual-breadcrumb-rank dual-breadcrumb-gap" aria-label="no rank">—</span>
        @endif
    </nav>

    <h1>${view.subject().displayName()}</h1>

    <p class="clade-concept-links">
        <a href="/concepts/clade">What is a clade?</a> ·
        <a href="/concepts/clade-taxonomy-relation">Clades vs. taxonomy</a>
    </p>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    @if(!view.children().isEmpty())
        <section>
            <h2>Direct descendants</h2>
            <ul class="clade-children">
                @for(var child : view.children())
                    <li><a href="/clades/${child.cladeSlug()}">${child.displayName()}</a></li>
                @endfor
            </ul>
        </section>
    @endif
`)
