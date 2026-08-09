@import com.naturalist.soil.observation.NutrientReading
@param String label
@param NutrientReading reading
<tr><td>${label}</td><td>${reading.value().toString()}</td><td>${reading.unit().symbol()}</td></tr>
