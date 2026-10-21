package org.openpnp.spi;

import org.apache.commons.lang3.NotImplementedException;
import org.openpnp.model.PickOffsetCorrection;

/**
 * Implemented by feeders that carry a reusable {@link PickOffsetCorrection}
 * for PickOffsetCorrection's shared UI code.
 */
public interface PickOffsetCorrectableFeeder {
    PickOffsetCorrection getPickOffsetCorrection();

    /**
     * Folds the currently learned correction offset into the feeder's stored pick position and resets
     * the correction, leaving the actual pick point unchanged. Lets an operator make a converged
     * correction permanent instead of keeping it as a runtime overlay.
     */
    default void bakePickOffsetCorrectionIntoPosition() throws Exception {
        throw new NotImplementedException();
    }
}
