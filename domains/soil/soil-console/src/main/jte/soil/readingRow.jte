@import com.naturalist.soil.observation.NutrientReading
@import java.util.Optional
@param String label
@param Optional<NutrientReading> reading
@if(reading.isPresent())
    !{var r = reading.get();}
    <tr><td>${label}</td><td>${r.value().toString()}</td><td>${r.unit().symbol()}</td></tr>
@else
    <tr class="not-reported"><td>${label}</td><td title="This lab did not report this nutrient. Not the same as a measured zero.">not reported</td><td></td></tr>
@endif
