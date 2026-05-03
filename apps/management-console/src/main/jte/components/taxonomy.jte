@import com.naturalist.taxonomy.TaxonomicClassification

@param TaxonomicClassification taxonomy

<p class="taxonomy">
    ${taxonomy.order().value()} &middot; ${taxonomy.family().value()}
    @if(taxonomy.genus() != null)
        &middot; <em>${taxonomy.genus().value()}
        @if(taxonomy.species() != null)
            ${taxonomy.species().value()}
        @endif
    </em>
    @endif
</p>
