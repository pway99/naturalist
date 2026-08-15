@import com.naturalist.garden.PlantedZone
@import java.time.LocalDate
@import java.util.List

@param List<PlantedZone> beds
@param LocalDate today

@template.layout.page(title = "Garden", content = @`
    <h1>Garden</h1>
    <p>What is growing at Oak Vista, bed by bed. A bed appears here once something has been
        planted in it — garden keeps no catalog of beds, only of plantings.</p>

    <div class="entity-grid">
        @for(var bed : beds)
            !{var growing = bed.activeOn(today);}
            <article>
                <header>
                    <a href="/garden/beds/${bed.zoneName().value()}">
                        <strong>${bed.zoneName().value()}</strong>
                    </a>
                </header>
                <dl class="taxonomy">
                    <dt>growing now</dt>
                    @if(growing.isEmpty())
                        <dd class="not-reported">nothing in the ground</dd>
                    @else
                        <dd>${growing.size()} planting(s)</dd>
                    @endif
                    <dt>recorded</dt>
                    <dd>${bed.plantings().size()} planting(s), ${bed.plantCount()} plant(s) counted</dd>
                    <dt>species</dt>
                    <dd>${bed.plants().size()}</dd>
                </dl>
            </article>
        @endfor
    </div>
`)
