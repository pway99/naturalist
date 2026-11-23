package com.naturalist.console.images;

import static org.assertj.core.api.Assertions.assertThat;

import com.naturalist.observation.ImageUpload;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Every classpath seed photo is already stripped: {@code withoutMetadata()} is the identity on it and it carries no
 * GPS. The seeder copies the seed photos into the image store, which serves them to the public as it serves any
 * upload, so none may ever carry EXIF, XMP or IPTC.
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
        ImageUpload upload = ImageUpload.of(resource.getContentAsByteArray());

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

    /** Every classpath seed photo is within the store's 1024 px photo edge, read from its header, so the seeder copies none that the store would not keep. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("seedImages")
    void seedImagesAreWithinThePhotoLimit(Resource resource) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(resource.getContentAsByteArray()))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            assertThat(readers.hasNext()).as("%s has an ImageIO reader", resource.getFilename()).isTrue();
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int longestEdge = Math.max(reader.getWidth(0), reader.getHeight(0));
                assertThat(longestEdge)
                        .as("%s is %d px on its longest edge; conform it to 1024", resource.getFilename(), longestEdge)
                        .isLessThanOrEqualTo(1024);
            } finally {
                reader.dispose();
            }
        }
    }
}
