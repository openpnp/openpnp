package org.openpnp.spi;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Placement;

public interface PnpJobProcessor extends JobProcessor {

    public static class JobPlacement {
        public enum Status {
            Pending,
            Processing,
            Skipped,
            Complete
        }

        public final BoardLocation boardLocation;
        public final Placement placement;
        public Status status = Status.Pending;

        public JobPlacement(BoardLocation boardLocation, Placement placement) {
            this.boardLocation = boardLocation;
            this.placement = placement;
        }

        public double getPartHeight() {
            return placement.getPart().getHeight().convertToUnits(LengthUnit.Millimeters)
                    .getValue();
        }

        public String getPartId() {
            return placement.getPart().getId();
        }

        @Override
        public String toString() {
            return placement.getId();
        }
    }
}
