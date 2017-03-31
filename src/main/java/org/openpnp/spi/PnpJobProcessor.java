package org.openpnp.spi;

import java.util.List;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Placement;

public interface PnpJobProcessor extends JobProcessor {

    public List<JobPlacement> getJobPlacementsById(String id);

    public List<JobPlacement> getJobPlacementsById(String id, JobPlacement.Status status);

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

        @Override
        public String toString() {
            return placement.getId();
        }
    }
}
