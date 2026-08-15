@import com.naturalist.garden.PlantedZone
@import java.time.LocalDate
@import java.util.List

@param List<PlantedZone> plantedZones
@param LocalDate today

@template.layout.page(title = "Garden", content = @`
    <h1>Garden</h1>
    <p>What is growing at Oak Vista, zone by zone. A zone appears here once something has been
        planted in it — garden keeps no catalog of places, only of plantings.</p>

    <div class="entity-grid">
        @for(var planted : plantedZones)
            !{var growing = planted.activeOn(today);}
            <article>
                <header>
                    <a href="/garden/planted-zones/${planted.zoneName().value()}">
                        <strong>${planted.zoneName().value()}</strong>
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
                    <dd>${planted.plantings().size()} planting(s), ${planted.plantCount()} plant(s) counted</dd>
                    <dt>species</dt>
                    <dd>${planted.plants().size()}</dd>
                </dl>
            </article>
        @endfor
    </div>
`)
