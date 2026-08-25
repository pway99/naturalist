@import com.naturalist.taxonomy.OrganismFeatureView
@import com.naturalist.plants.PlantFeature
@import com.naturalist.plants.PlantRankLabel
@import com.naturalist.plants.PlantRankName

@param OrganismFeatureView<PlantRankName, PlantFeature> features = null

@if(features != null && !features.groups().isEmpty())
    <section class="field-marks">
        <h2>Field Marks</h2>
        <p class="field-marks-lead">What the identification was based on, from most conspicuous to most diagnostic. Check these against your photo.</p>
        @for(var group : features.groups())
            <div class="field-mark-group">
                <h3>${PlantRankLabel.of(group.rank())} — ${group.rank().value()}</h3>
                <ul>
                    @for(var feature : group.features())
                        <li>${feature.value()}</li>
                    @endfor
                </ul>
            </div>
        @endfor
    </section>
@endif
