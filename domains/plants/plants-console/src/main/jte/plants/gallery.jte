@import com.naturalist.observation.OrganismImage
@import com.naturalist.plants.PlantImageId
@import com.naturalist.plants.PlantObservationId
@import com.naturalist.plants.PlantRankName
@import java.util.List

@param List<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> images = java.util.List.of()
@param String alt = ""

@if(!images.isEmpty())
    <section class="photos">
        <h2>Photos</h2>
        <div class="photo-grid">
            @for(int i = 0; i < images.size(); i++)
                <%-- Uploaded photos live under /uploads; bundled catalog images under /images.
                     Try uploads first, fall back once to the classpath copy. --%>
                <img src="/plants/uploads/${images.get(i).resourceName().value()}"
                     alt="${alt}"
                     loading="lazy"
                     onerror="this.onerror=null; this.src='/plants/images/${images.get(i).resourceName().value()}'">
            @endfor
        </div>
    </section>
@endif
