package com.naturalist.observation;

import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observability.Constraint;

import java.util.Collection;

/**
 * Thrown by {@link ImageStore#store} when the upload's body does not decode, with nothing written. It is the one
 * {@code InvariantViolationException} a caller may answer as a bad
 * photo: any other one raised while handling an upload is a fault, not the photo's. The refusal is counted where it
 * is thrown, so a caller that answers it does not count it again.
 */
public final class ImageRefusedException extends InvariantViolationException {
    public ImageRefusedException(String scope, Collection<? extends Constraint<?>> violations) {
        super(scope, violations);
    }
}
