@import com.naturalist.insects.console.FeatureGroup
@import java.util.List

@param List<FeatureGroup> featureGroups = List.of()

@if(!featureGroups.isEmpty())
    <section class="field-marks">
        <h2>Field Marks</h2>
        <p class="field-marks-lead">What the identification was based on, from
            most conspicuous to most diagnostic. Check these against your photo.</p>
        @for(var group : featureGroups)
            <div class="field-mark-group">
                <h3>${group.rankLabel()} — ${group.rankSlug()}</h3>
                <ul>
                    @for(var mark : group.marks())
                        <li>${mark}</li>
                    @endfor
                </ul>
            </div>
        @endfor
    </section>
@endif
