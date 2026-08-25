@import com.naturalist.taxonomy.OrganismFeatureView
@import com.naturalist.insects.InsectFeature
@import com.naturalist.insects.InsectRankName
@import com.naturalist.insects.RankLabel
@import com.naturalist.library.GlossaryLinker

@param OrganismFeatureView<InsectRankName, InsectFeature> features = null
@param GlossaryLinker glossaryLinker = GlossaryLinker.none()

@if(features != null && !features.groups().isEmpty())
    <section class="field-marks">
        <h2>Field Marks</h2>
        <p class="field-marks-lead">$unsafe{glossaryLinker.linkHtml("What the identification was based on, from most conspicuous to most diagnostic. Check these against your photo.", "field-marks-lead")}</p>
        @for(var group : features.groups())
            <div class="field-mark-group">
                <h3>${RankLabel.of(group.rank())} — ${group.rank().value()}</h3>
                <ul>
                    @for(int i = 0; i < group.features().size(); i++)
                        <li>$unsafe{glossaryLinker.linkHtml(group.features().get(i).value(), group.rank().value() + "-" + i)}</li>
                    @endfor
                </ul>
            </div>
        @endfor
    </section>
@endif
