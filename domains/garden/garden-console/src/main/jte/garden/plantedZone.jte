@import com.naturalist.garden.PlantedZone
@import java.time.LocalDate

@param PlantedZone planted
@param LocalDate today

!{var title = planted.coversWholeZone() ? planted.zoneName().value() : planted.subZoneName().value();}

@template.layout.page(title = title, content = @`
    <h1>${title}</h1>
    <dl class="taxonomy">
        <dt>zone</dt>
        <dd><a href="/garden/planted-zones/${planted.zoneName().value()}">${planted.zoneName().value()}</a>@if(!planted.coversWholeZone()) · row ${planted.subZoneName().value()}@endif</dd>
        <dt>growing now</dt>
        <dd>${planted.activeOn(today).size()} of ${planted.plantings().size()} planting(s)</dd>
    </dl>

    @if(planted.coversWholeZone())
        <p class="fraction-note">A zone holds whatever was planted in it. Rows are where plantings
            are, not what they contain — one row can carry two different plants.</p>
    @endif

    <h2>Plantings</h2>
    <table>
        <thead>
            <tr><th>Plant</th><th>Variety</th><th>Row</th><th>Plants</th><th>Planted</th><th>Removed</th></tr>
        </thead>
        <tbody>
            @for(var p : planted.plantings().stream().toList())
                <tr>
                    <td>@if(p.plantName() != null)${p.plantName().value()}<span class="badge">${p.plantName().rank().name()}</span>@else<span class="not-reported">not recorded</span>@endif</td>
                    @if(p.isVarietyKnown())
                        <td>${p.cultivarName().value()}</td>
                    @else
                        <td class="not-reported" title="The plant is known; the variety was never recorded. Not the same as having no variety.">not recorded</td>
                    @endif
                    @if(p.isZoneScoped())
                        <td class="not-reported">whole zone</td>
                    @else
                        <td><a href="/garden/planted-zones/${planted.zoneName().value()}/${p.subZoneName().value()}">${p.subZoneName().value()}</a></td>
                    @endif
                    <td>@if(p.plantCount() != null)${p.plantCount()}@else<span class="not-reported">uncounted</span>@endif</td>
                    <td>${p.plantedDate().toString()}</td>
                    @if(p.removedDate() != null)
                        <td>${p.removedDate().toString()}</td>
                    @else
                        <td class="growing">still growing</td>
                    @endif
                </tr>
            @endfor
        </tbody>
    </table>

    @if(!planted.cultivars().isEmpty())
        <p class="assessment-source">Varieties link through the plants catalog, which owns their
            breeding status, fruit type and seed-saving policy. Garden records only that they were
            planted.</p>
    @endif
`)
