@import com.naturalist.library.GlossaryTerm
@param GlossaryTerm term

@template.layout.page(title = term.term(), content = @`
    <nav class="breadcrumb-trail" aria-label="Breadcrumb">
        <span class="breadcrumb-segment"><a href="/glossary">Glossary</a></span>
        <span class="breadcrumb-sep" aria-hidden="true">›</span>
        <span class="breadcrumb-segment breadcrumb-current"><strong>${term.term()}</strong></span>
    </nav>
    <h1>${term.term()}</h1>
    <p class="glossary-definition">${term.definition()}</p>
    @if(term.example() != null)
        <p class="glossary-example"><em>${term.example()}</em></p>
    @endif
    <p><a href="/glossary">← Back to the glossary</a></p>
`)
