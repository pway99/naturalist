@import com.naturalist.console.admin.AdminResilienceController.Group
@import java.util.List

@param List<Group> groups

@template.layout.page(title = "Resilience", content = @`
    @template.admin.nav(current = "resilience")
    <section class="admin-resilience">
        <h1>Resilience</h1>
        <p>Strategies registered with the kernel <code>Resilience</code> facade,
            grouped by primitive. A name listed here is one a
            <code>&#64;Resilient(name = ...)</code> call site can reference.</p>

        @for(var group : groups)
            <section class="admin-resilience-group">
                <h2>${group.primitive()}</h2>
                @if(group.names().isEmpty())
                    <p><em>None registered.</em></p>
                @else
                    <ul>
                        @for(var name : group.names())
                            <li><code>${name}</code></li>
                        @endfor
                    </ul>
                @endif
            </section>
        @endfor
    </section>
`)
