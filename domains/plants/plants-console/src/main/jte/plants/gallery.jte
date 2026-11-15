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
                <%-- The tile links to the photo; photo-view.js opens it over the page. --%>
                <a href="/plants/images/${images.get(i).resourceName().value()}" data-photo-view>
                    <img src="/plants/images/tile/${images.get(i).resourceName().value()}"
                         alt="${alt}"
                         loading="lazy"
                         onerror="this.onerror=null;this.src='/images/photo-not-found.svg'">
                </a>
            @endfor
        </div>
    </section>
@endif
