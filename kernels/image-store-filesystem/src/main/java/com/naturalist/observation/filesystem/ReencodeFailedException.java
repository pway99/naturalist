package com.naturalist.observation.filesystem;

/**
 * A decoded image {@link PhotoScaler#fitJpeg} could not draw upright or re-encode: a fault, never a refusal of the
 * photo. It is counted once where it is thrown ({@code PhotoScaler.fitJpeg.encoded}), so the store's catches name this
 * type alone to pass it through uncounted; any other {@code IllegalStateException} is still counted there.
 */
final class ReencodeFailedException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    ReencodeFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
