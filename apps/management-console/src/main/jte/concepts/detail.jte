@import com.naturalist.library.Concept

@param Concept concept
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = concept.title(), content = @`
    <nav class="breadcrumb-trail" aria-label="Breadcrumb">
        <span class="breadcrumb-segment"><a href="/concepts">Concepts</a></span>
        <span class="breadcrumb-sep" aria-hidden="true">›</span>
        <span class="breadcrumb-segment breadcrumb-current"><strong>${concept.title()}</strong></span>
    </nav>
    <h1>${concept.title()}</h1>
    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)
    <p><a href="/clades">Browse the tree of life →</a></p>
`)
