@import com.naturalist.observation.OrganismImage
@import com.naturalist.insects.InsectImageId
@import com.naturalist.insects.InsectObservationId
@import com.naturalist.insects.InsectRankName
@import java.util.List

@param List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> images
@param String linkUrl
@param String alt

<a href="${linkUrl}" class="card-image-link">
    <figure class="card-image" data-rotate="${images.size() > 1}">
        @for(int i = 0; i < images.size(); i++)
            <%-- One endpoint serves uploads and bundled catalog images alike. --%>
            <img src="/insects/images/${images.get(i).resourceName().value()}"
                 alt="${alt}"
                 loading="lazy"
                 class="${i == 0 ? "card-image-active" : ""}">
        @endfor
    </figure>
</a>
