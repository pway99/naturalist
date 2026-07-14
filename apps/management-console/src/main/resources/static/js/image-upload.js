(function () {
    'use strict';

    /**
     * Reads EXIF data from a JPEG file to extract GPS coordinates and date.
     * Minimal parser — handles only GPS and DateTimeOriginal tags.
     */
    function readExif(file) {
        return new Promise(function (resolve) {
            var reader = new FileReader();
            reader.onload = function (e) {
                var view = new DataView(e.target.result);
                // Check for JPEG SOI marker
                if (view.getUint16(0) !== 0xFFD8) {
                    resolve({});
                    return;
                }
                var offset = 2;
                var result = {};
                while (offset < view.byteLength - 2) {
                    var marker = view.getUint16(offset);
                    if (marker === 0xFFE1) { // APP1 (EXIF)
                        var exifData = parseExifApp1(view, offset + 4);
                        if (exifData.lat !== undefined && exifData.lng !== undefined) {
                            result.lat = exifData.lat;
                            result.lng = exifData.lng;
                        }
                        if (exifData.dateTime) result.dateTime = exifData.dateTime;
                        break;
                    }
                    var size = view.getUint16(offset + 2);
                    offset += 2 + size;
                }
                resolve(result);
            };
            reader.readAsArrayBuffer(file);
        });
    }

    function parseExifApp1(view, start) {
        // MVP: returns empty — the user manually enters location in the text field.
        // A full EXIF IFD parser (byte-order detection, IFD0 traversal, GPS sub-IFD
        // with rational-to-decimal conversion) is ~150 lines. Implement when the
        // manual-entry friction is named. Alternatively, use a small library like
        // exif-js or piexifjs.
        return {};
    }

    /**
     * Resizes an image to fit within maxDim on its longest side,
     * re-encodes as JPEG quality 0.85, returns a Blob.
     */
    function resizeImage(file, maxDim) {
        return new Promise(function (resolve) {
            var img = new window.Image();
            img.onload = function () {
                var w = img.width, h = img.height;
                if (w > maxDim || h > maxDim) {
                    if (w > h) { h = Math.round(h * maxDim / w); w = maxDim; }
                    else { w = Math.round(w * maxDim / h); h = maxDim; }
                }
                var canvas = document.createElement('canvas');
                canvas.width = w;
                canvas.height = h;
                canvas.getContext('2d').drawImage(img, 0, 0, w, h);
                canvas.toBlob(function (blob) { resolve(blob); }, 'image/jpeg', 0.85);
            };
            img.src = URL.createObjectURL(file);
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        var form = document.getElementById('identify-form');
        if (!form) return;

        var fileInput = form.querySelector('input[type="file"]');
        var locationInput = form.querySelector('input[name="location"]');
        var dateInput = form.querySelector('input[name="capturedAt"]');
        var preview = document.getElementById('image-preview');

        fileInput.addEventListener('change', function () {
            var file = fileInput.files[0];
            if (!file) return;

            // Show preview
            if (preview) {
                preview.src = URL.createObjectURL(file);
                preview.style.display = 'block';
            }

            // Extract EXIF
            readExif(file).then(function (exif) {
                if (exif.lat !== undefined && exif.lng !== undefined && locationInput) {
                    // For MVP, show coordinates; user edits to a place name
                    locationInput.value = exif.lat.toFixed(4) + ', ' + exif.lng.toFixed(4);
                }
                if (exif.dateTime && dateInput) {
                    dateInput.value = exif.dateTime;
                }
            });
        });

        form.addEventListener('submit', function (e) {
            e.preventDefault();
            var file = fileInput.files[0];
            if (!file) return;

            var submitBtn = form.querySelector('button[type="submit"]');
            submitBtn.disabled = true;
            submitBtn.textContent = 'Identifying...';

            resizeImage(file, 1024).then(function (blob) {
                var formData = new FormData();
                formData.append('image', blob, file.name);
                // Copy text fields
                var fields = form.querySelectorAll('input[type="text"], input[type="hidden"], textarea');
                fields.forEach(function (f) { formData.append(f.name, f.value); });

                return fetch(form.action, { method: 'POST', body: formData });
            }).then(function (response) {
                if (response.redirected) {
                    window.location.href = response.url;
                } else {
                    window.location.reload();
                }
            }).catch(function (err) {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Identify';
                alert('Identification failed: ' + err.message);
            });
        });
    });
}());
