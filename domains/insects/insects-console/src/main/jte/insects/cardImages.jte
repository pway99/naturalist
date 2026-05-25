@import com.naturalist.insects.InsectImage
@import java.util.List

@param List<InsectImage> images
@param String linkUrl
@param String alt

<a href="${linkUrl}" class="card-image-link">
    <figure class="card-image" data-rotate="${images.size() > 1}">
        @for(int i = 0; i < images.size(); i++)
            <img src="/insects/images/${images.get(i).resourceName().value()}"
                 alt="${alt}"
                 loading="lazy"
                 class="${i == 0 ? "card-image-active" : ""}">
        @endfor
    </figure>
</a>
