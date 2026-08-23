@import com.naturalist.library.CladeView
@import com.naturalist.library.clade.CladeUrls
@import java.util.Map

@param CladeView view
@param Map<String, String> rankLinks = java.util.Map.of()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = view.subject().displayName(), content = @`
    <div class="context-bar">
        <nav class="breadcrumb-trail" aria-label="Phylogenetic lineage">
            @for(var step : view.ancestry())
                <span class="breadcrumb-segment">
                    @if(step.rank().isPresent())
                        !{var r = step.rank().get().name();}
                        <span class="breadcrumb-rank"><a href="${rankLinks.get(step.cladeSlug())}">${r.charAt(0) + r.substring(1).toLowerCase()}</a></span>
                    @endif
                    <a href="${CladeUrls.of(step.cladeSlug())}">${step.displayName()}</a>
                </span>
                <span class="breadcrumb-sep" aria-hidden="true">›</span>
            @endfor
            <span class="breadcrumb-segment">
                @if(view.subject().rank().isPresent())
                    !{var sr = view.subject().rank().get().name();}
                    <span class="breadcrumb-rank"><a href="${rankLinks.get(view.subject().cladeSlug())}">${sr.charAt(0) + sr.substring(1).toLowerCase()}</a></span>
                @endif
                <span class="breadcrumb-current"><strong>${view.subject().displayName()}</strong></span>
            </span>
            @template.components.infoPopover(
                id = "clade-rank-help",
                label = "Why do some of these clades show a Linnaean rank?",
                text = "These names are <strong>clades</strong> — real branches of the tree of life. A few"
                    + " also carry a famous Linnaean rank (Kingdom, Phylum, Class…), shown above the name;"
                    + " branches like <em>Eukaryota</em> and <em>Holometabola</em> have no such rank.",
                moreHref = "/concepts/clade-taxonomy-relation",
                moreLabel = "More on clades vs. taxonomy →")
        </nav>
    </div>

    <h1>${view.subject().displayName()}</h1>

    <p class="clade-concept-links">
        <a href="/concepts/clade">What is a clade?</a> ·
        <a href="/concepts/clade-taxonomy-relation">Clades vs. taxonomy</a> ·
        <a href="/concepts/taxonomic-rank">Rank ladder</a>
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
                    <li><a href="${CladeUrls.of(child.cladeSlug())}">${child.displayName()}</a></li>
                @endfor
            </ul>
        </section>
    @endif
`)
