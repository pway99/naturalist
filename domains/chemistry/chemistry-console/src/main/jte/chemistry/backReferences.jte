@import com.naturalist.chemistry.catalog.BackReferencesViewModel

@param BackReferencesViewModel backReferences

@if(!backReferences.isEmpty())
    <section class="back-references">
        <h2>Found in</h2>
        @for(var group : backReferences.groups())
            <div class="back-references-group">
                <h3>${group.domainDisplayName()}</h3>
                <ul>
                    @for(var link : group.links())
                        <li><a href="${link.url()}">${link.label()}</a></li>
                    @endfor
                </ul>
            </div>
        @endfor
    </section>
@endif
