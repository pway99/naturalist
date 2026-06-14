@import com.naturalist.console.clade.CladesController.CladeLink
@import java.util.List

@param String displayName
@param String slug
@param List<CladeLink> ancestry = List.of()
@param List<CladeLink> children = List.of()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = displayName, content = @`
    <nav class="breadcrumb" aria-label="Clade ancestry">
        <a href="/clades">Tree of Life</a>
        @for(var ancestor : ancestry)
            &rsaquo; <a href="/clades/${ancestor.slug()}">${ancestor.displayName()}</a>
        @endfor
        &rsaquo; <span aria-current="page">${displayName}</span>
    </nav>

    <h1>${displayName}</h1>

    <p class="clade-concept-links">
        <a href="/concepts/clade">What is a clade?</a> ·
        <a href="/concepts/clade-taxonomy-relation">Clades vs. taxonomy</a>
    </p>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    @if(!children.isEmpty())
        <section>
            <h2>Direct descendants</h2>
            <ul class="clade-children">
                @for(var child : children)
                    <li><a href="/clades/${child.slug()}">${child.displayName()}</a></li>
                @endfor
            </ul>
        </section>
    @endif
`)
