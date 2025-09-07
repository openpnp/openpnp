package org.openpnp.spi;

import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;

public interface PnpJobProcessor extends JobProcessor {
    public static class JobPlacement extends AbstractModelObject {
        public enum Status {
            Pending,
            Processing,
            Errored,
            Complete
        }

        private final BoardLocation boardLocation;
        private final Placement placement;
        private Status status = Status.Pending;
        private Exception error;
        private int feederIndex;
        private Location plannedPickLocation;
        private int processingCount;

        public JobPlacement(BoardLocation boardLocation, Placement placement) {
            this.boardLocation = boardLocation;
            this.placement = placement;
            this.processingCount = 0;
        }

        public BoardLocation getBoardLocation() {
            return boardLocation;
        }

        public Placement getPlacement() {
            return placement;
        }

        public int getRank() {
            return placement.getRank();
        }

        public void setStatus(Status status) {
            Object oldValue = this.status;
            this.status = status;
            if (status==Status.Processing) {
                processingCount += 1;
            }
            firePropertyChange("status", oldValue, status);
        }

        public Status getStatus() {
            return status;
        }

        // A count of the number of times this placement has been in "Processing" status.
        public int getProcessingCount() {
            return processingCount;
        }

        /**
         * Set the error, and as a side effect, set the status to Errored.
         * @param error
         */
        public void setError(Exception error) {
            Object oldValue = this.error;
            this.error = error;
            firePropertyChange("error", oldValue, error);
            setStatus(Status.Errored);
        }

        public Exception getError() {
            return error;
        }

        public double getPartHeight() {
            return (placement.getPart() != null 
                    ? placement.getPart().getHeight().convertToUnits(LengthUnit.Millimeters).getValue()
                            : 0.0);
        }

        public String getPartId() {
            return (placement.getPart() != null 
                    ? placement.getPart().getId()
                            : "");
        }
        
        public String getBoardId() {
            return boardLocation.getId();
        }

        public int getFeederIndex() {
            return feederIndex;
        }
        
        public void setFeederIndex(int index) {
            this.feederIndex = index;
        }

        public Location getPlannedPickLocation() {
            return plannedPickLocation;
        }

        public void setPlannedPickLocation(Location l) {
            plannedPickLocation = l;
        }

        @Override
        public String toString() {
            return placement.getId();
        }
    }
}
