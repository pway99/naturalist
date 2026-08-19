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
            <%-- Uploaded (vision) photos live under /uploads; bundled catalog images
                 under /images. Try uploads first, fall back once to the classpath copy. --%>
            <img src="/insects/uploads/${images.get(i).resourceName().value()}"
                 alt="${alt}"
                 loading="lazy"
                 onerror="this.onerror=null; this.src='/insects/images/${images.get(i).resourceName().value()}'"
                 class="${i == 0 ? "card-image-active" : ""}">
        @endfor
    </figure>
</a>
