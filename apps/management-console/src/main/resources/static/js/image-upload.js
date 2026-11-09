(function () {
    'use strict';

    var CELLS_PER_DEGREE = 24; // the 2.5-arc-minute lattice; mirrors the server's GridCell

    /**
     * Reads the GPS fix from a JPEG's EXIF block. The coordinates stay in this
     * function: callers turn them into a coarse cell and discard them.
     * Resolves {lat, lng} (decimal degrees), or {} when there is no usable fix.
     */
    function readExif(file) {
        return new Promise(function (resolve) {
            var reader = new FileReader();
            reader.onload = function (e) {
                resolve(readExifFromArrayBuffer(e.target.result));
            };
            reader.onerror = function () { resolve({}); };
            reader.readAsArrayBuffer(file);
        });
    }

    /** Walks the JPEG segments for the Exif APP1 (skipping e.g. an XMP APP1) and parses its GPS fix. */
    function readExifFromArrayBuffer(buffer) {
        try {
            var view = new DataView(buffer);
            if (view.getUint16(0) !== 0xFFD8) return {}; // not a JPEG
            var offset = 2;
            while (offset + 4 <= view.byteLength) {
                if (view.getUint8(offset) !== 0xFF) return {};
                var marker = view.getUint16(offset);
                if (marker === 0xFFFF) { offset += 1; continue; } // fill byte
                if (marker === 0xFFDA || marker === 0xFFD9) return {}; // image data or end: no Exif seen
                var size = view.getUint16(offset + 2);
                if (size < 2) return {};
                if (marker === 0xFFE1 && hasExifHeader(view, offset + 4)) {
                    return parseExifApp1(view, offset + 10);
                }
                offset += 2 + size;
            }
        } catch (err) {
            // truncated or malformed: treat as no fix
        }
        return {};
    }

    function hasExifHeader(view, at) {
        return view.getUint8(at) === 0x45 && view.getUint8(at + 1) === 0x78 // "Ex"
            && view.getUint8(at + 2) === 0x69 && view.getUint8(at + 3) === 0x66 // "if"
            && view.getUint8(at + 4) === 0 && view.getUint8(at + 5) === 0;
    }

    /** Parses the TIFF structure starting at tiff; returns {lat, lng} or {} (never throws). */
    function parseExifApp1(view, tiff) {
        try {
            var order = view.getUint16(tiff);
            var little;
            if (order === 0x4949) little = true;       // "II"
            else if (order === 0x4D4D) little = false; // "MM"
            else return {};
            if (view.getUint16(tiff + 2, little) !== 0x002A) return {};

            var ifd0 = view.getUint32(tiff + 4, little);
            var gpsPointer = findEntry(view, tiff, ifd0, 0x8825, little);
            if (gpsPointer < 0) return {};
            var gps = view.getUint32(gpsPointer + 8, little);

            var latRef = findEntry(view, tiff, gps, 1, little);
            var latVal = findEntry(view, tiff, gps, 2, little);
            var lonRef = findEntry(view, tiff, gps, 3, little);
            var lonVal = findEntry(view, tiff, gps, 4, little);
            if (latRef < 0 || latVal < 0 || lonRef < 0 || lonVal < 0) return {};

            var lat = readDegrees(view, tiff, latVal, little);
            var lng = readDegrees(view, tiff, lonVal, little);
            var latLetter = String.fromCharCode(view.getUint8(latRef + 8));
            var lonLetter = String.fromCharCode(view.getUint8(lonRef + 8));
            if (latLetter === 'S') lat = -lat;
            else if (latLetter !== 'N') return {};
            if (lonLetter === 'W') lng = -lng;
            else if (lonLetter !== 'E') return {};

            if (!isFinite(lat) || !isFinite(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180) return {};
            return { lat: lat, lng: lng };
        } catch (err) {
            return {};
        }
    }

    /** Absolute position of the 12-byte entry for tag in the IFD at ifdOffset (relative to tiff), or -1. */
    function findEntry(view, tiff, ifdOffset, tag, little) {
        var start = tiff + ifdOffset;
        var count = view.getUint16(start, little);
        for (var i = 0; i < count; i++) {
            var entry = start + 2 + i * 12;
            if (view.getUint16(entry, little) === tag) return entry;
        }
        return -1;
    }

    /** Reads a 3 x RATIONAL (degrees, minutes, seconds) value of the entry; throws on a bad shape. */
    function readDegrees(view, tiff, entry, little) {
        if (view.getUint16(entry + 2, little) !== 5 || view.getUint32(entry + 4, little) !== 3) {
            throw new Error('not 3 rationals');
        }
        var at = tiff + view.getUint32(entry + 8, little); // 24 bytes: too long to be inline
        var parts = [];
        for (var i = 0; i < 3; i++) {
            var den = view.getUint32(at + i * 8 + 4, little);
            if (den === 0) throw new Error('zero denominator');
            parts.push(view.getUint32(at + i * 8, little) / den);
        }
        return parts[0] + parts[1] / 60 + parts[2] / 3600;
    }

    /** The lattice cell containing the point; the same floor arithmetic as the server's GridCell. */
    function cellFor(lat, lng) {
        return {
            latIndex: Math.floor(lat * CELLS_PER_DEGREE),
            lonIndex: Math.floor(lng * CELLS_PER_DEGREE)
        };
    }

    function edge(value) { return Math.abs(value).toFixed(2); }

    /** Same text as the server's GridCell.describe(): "45.50–45.54°N, 122.71–122.67°W". */
    function describeCell(latIndex, lonIndex) {
        var south = latIndex / CELLS_PER_DEGREE, north = (latIndex + 1) / CELLS_PER_DEGREE;
        var west = lonIndex / CELLS_PER_DEGREE, east = (lonIndex + 1) / CELLS_PER_DEGREE;
        return edge(south) + '–' + edge(north) + '°' + (south >= 0 ? 'N' : 'S') + ', '
            + edge(west) + '–' + edge(east) + '°' + (west >= 0 ? 'E' : 'W');
    }

    window.naturalistImageUpload = {
        readExifFromArrayBuffer: readExifFromArrayBuffer,
        cellFor: cellFor,
        describeCell: describeCell
    };

    /**
     * Resizes an image to fit within maxDim on its longest side,
     * re-encodes as JPEG quality 0.85, returns a Blob.
     */
    function resizeImage(file, maxDim) {
        return new Promise(function (resolve, reject) {
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
            img.onerror = function () { reject(new Error('This browser cannot read that photo. Choose a JPEG, PNG or WebP image.')); };
            img.src = URL.createObjectURL(file);
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        var form = document.getElementById('identify-form');
        if (!form) return;

        var fileInput = form.querySelector('input[type="file"]');
        var preview = document.getElementById('image-preview');
        var areaLat = form.querySelector('input[name="areaLat"]');
        var areaLon = form.querySelector('input[name="areaLon"]');
        var areaLine = document.getElementById('area-line');
        var areaText = document.getElementById('area-text');
        var areaClear = document.getElementById('area-clear');
        var latestChoice = 0; // a slow read of an earlier photo must not overwrite a later choice

        function clearArea() {
            if (areaLat) areaLat.value = '';
            if (areaLon) areaLon.value = '';
            if (areaText) areaText.textContent = '';
            if (areaLine) areaLine.hidden = true;
        }

        function showArea(cell) {
            if (!areaLat || !areaLon || !areaLine || !areaText) return;
            areaLat.value = String(cell.latIndex);
            areaLon.value = String(cell.lonIndex);
            areaText.textContent = describeCell(cell.latIndex, cell.lonIndex);
            areaLine.hidden = false;
        }

        if (areaClear) areaClear.addEventListener('click', function () { latestChoice++; clearArea(); });

        fileInput.addEventListener('change', function () {
            var choice = ++latestChoice;
            clearArea(); // a new photo never inherits the previous photo's area
            var file = fileInput.files[0];
            if (!file) return;

            // Show preview
            if (preview) {
                preview.src = URL.createObjectURL(file);
                preview.style.display = 'block';
            }

            // Only the coarse cell is kept; the exact fix is dropped here.
            readExif(file).then(function (exif) {
                if (choice !== latestChoice) return;
                if (exif.lat !== undefined && exif.lng !== undefined) {
                    showArea(cellFor(exif.lat, exif.lng));
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
