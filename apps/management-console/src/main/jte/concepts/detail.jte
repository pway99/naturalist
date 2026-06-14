@import com.naturalist.library.Concept

@param Concept concept
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = concept.title(), content = @`
    <nav class="breadcrumb" aria-label="Breadcrumb"><a href="/concepts">Concepts</a></nav>
    <h1>${concept.title()}</h1>
    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)
    <p><a href="/clades">Browse the tree of life →</a></p>
`)
