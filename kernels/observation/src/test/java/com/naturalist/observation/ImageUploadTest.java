package com.naturalist.observation;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageUploadTest {
    private static final Observer observer = Observer.forClass(ImageUploadTest.class);

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    static final byte[] WEBP = {0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50, 0, 0, 0, 0};

    static final byte[] HEIC = {0, 0, 0, 0x18, 0x66, 0x74, 0x79, 0x70, 0x68, 0x65, 0x69, 0x63, 0, 0, 0, 0};

    @Test
    void jpeg_isValidWithMediaTypeAndExtension() {
        var mo = observer.forMethod("jpeg_isValidWithMediaTypeAndExtension");
        ImageUpload upload = ImageUpload.of(JPEG);
        InvariantObservation result = mo.observable(upload, "upload");
        assertThat(result.violations()).isEmpty();
        assertThat(upload.mediaType()).isEqualTo("image/jpeg");
        assertThat(upload.extension()).isEqualTo(".jpg");
    }

    @Test
    void png_isValid() {
        var mo = observer.forMethod("png_isValid");
        ImageUpload upload = ImageUpload.of(PNG);
        InvariantObservation result = mo.observable(upload, "upload");
        assertThat(result.violations()).isEmpty();
        assertThat(upload.mediaType()).isEqualTo("image/png");
        assertThat(upload.extension()).isEqualTo(".png");
    }

    @Test
    void webp_isValid() {
        var mo = observer.forMethod("webp_isValid");
        ImageUpload upload = ImageUpload.of(WEBP);
        InvariantObservation result = mo.observable(upload, "upload");
        assertThat(result.violations()).isEmpty();
        assertThat(upload.mediaType()).isEqualTo("image/webp");
        assertThat(upload.extension()).isEqualTo(".webp");
    }

    @Test
    void heic_isInvalid_format() {
        var mo = observer.forMethod("heic_isInvalid_format");
        InvariantObservation result = mo.observable(ImageUpload.of(HEIC), "upload");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint())).containsExactlyInAnyOrder(".upload.format");
    }

    @Test
    void oversize_isInvalid_size() {
        var mo = observer.forMethod("oversize_isInvalid_size");
        byte[] big = new byte[ImageUpload.MAX_BYTES + 1];
        System.arraycopy(JPEG, 0, big, 0, JPEG.length);
        InvariantObservation result = mo.observable(ImageUpload.of(big), "upload");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint())).containsExactlyInAnyOrder(".upload.size");
    }

    @Test
    void tooShort_isInvalid_format() {
        var mo = observer.forMethod("tooShort_isInvalid_format");
        byte[] shortBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        InvariantObservation result = mo.observable(ImageUpload.of(shortBytes), "upload");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint())).containsExactlyInAnyOrder(".upload.format");
    }

    @Test
    void equality_isByContent() {
        assertThat(ImageUpload.of(JPEG)).isEqualTo(ImageUpload.of(JPEG.clone()));
        assertThat(ImageUpload.of(JPEG).hashCode()).isEqualTo(ImageUpload.of(JPEG.clone()).hashCode());
    }

    @Test
    void equality_differsByContent() {
        assertThat(ImageUpload.of(JPEG)).isNotEqualTo(ImageUpload.of(PNG));
    }

    @Test
    void of_nullNeverThrows() {
        var mo = observer.forMethod("of_nullNeverThrows");
        ImageUpload upload = ImageUpload.of(null);
        assertThat(upload.mediaType()).isNull();
        InvariantObservation result = mo.observable(upload, "upload");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint())).contains(".upload.format");
    }
}
