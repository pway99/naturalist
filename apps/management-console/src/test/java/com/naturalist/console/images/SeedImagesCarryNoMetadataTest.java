package com.naturalist.console.images;

import static org.assertj.core.api.Assertions.assertThat;

import com.naturalist.observation.ImageUpload;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Every classpath seed photo is already stripped: {@code withoutMetadata()} is the identity on it and it carries no
 * GPS. The seed photos are served to the public through the image store's classpath fallback, so none may ever carry
 * EXIF, XMP or IPTC.
 */
class SeedImagesCarryNoMetadataTest {

    private static final PathMatchingResourcePatternResolver RESOLVER = new PathMatchingResourcePatternResolver();

    static Stream<Resource> seedImages() throws IOException {
        return Stream.concat(
                        Stream.of(RESOLVER.getResources("classpath*:insects/images/*")),
                        Stream.of(RESOLVER.getResources("classpath*:plants/images/*")))
                .filter(Resource::isReadable);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("seedImages")
    void seedImage_isAlreadyStripped(Resource resource) throws IOException {
        var upload = ImageUpload.of(resource.getContentAsByteArray());

        assertThat(upload.isValid()).as("%s is a valid image", resource.getFilename()).isTrue();
        assertThat(upload.gps()).as("%s GPS", resource.getFilename()).isEmpty();
        assertThat(upload.withoutMetadata().bytes())
                .as("%s carries metadata; rewrite it with ImageUpload.withoutMetadata()", resource.getFilename())
                .isEqualTo(upload.bytes());
    }

    @Test
    void thereAreSeedImages() throws IOException {
        assertThat(seedImages().count()).isGreaterThanOrEqualTo(18);
    }
}
