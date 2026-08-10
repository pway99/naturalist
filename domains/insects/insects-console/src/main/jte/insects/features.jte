@import com.naturalist.insects.console.FeatureGroup
@import com.naturalist.library.console.GlossaryLinker
@import java.util.List

@param List<FeatureGroup> featureGroups = List.of()
@param GlossaryLinker glossaryLinker = GlossaryLinker.none()

@if(!featureGroups.isEmpty())
    <section class="field-marks">
        <h2>Field Marks</h2>
        <p class="field-marks-lead">$unsafe{glossaryLinker.linkHtml("What the identification was based on, from most conspicuous to most diagnostic. Check these against your photo.", "field-marks-lead")}</p>
        @for(var group : featureGroups)
            <div class="field-mark-group">
                <h3>${group.rankLabel()} — ${group.rankSlug()}</h3>
                <ul>
                    @for(int i = 0; i < group.marks().size(); i++)
                        <li>$unsafe{glossaryLinker.linkHtml(group.marks().get(i), group.rankSlug() + "-" + i)}</li>
                    @endfor
                </ul>
            </div>
        @endfor
    </section>
@endif
