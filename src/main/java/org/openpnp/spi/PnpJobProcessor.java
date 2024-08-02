package org.openpnp.spi;

import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
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
        private final NozzleTip nozzleTip;    // nozzleTip to use for this placement or null if unspecified
        private Status status = Status.Pending;
        private Exception error;
        private int feederIndex;

        public JobPlacement(JobPlacement jobPlacement, NozzleTip nozzleTip) {
            this.boardLocation = jobPlacement.boardLocation;
            this.placement = jobPlacement.placement;
            this.nozzleTip = nozzleTip;
        }

        public JobPlacement(BoardLocation boardLocation, Placement placement) {
            this.boardLocation = boardLocation;
            this.placement = placement;
            this.nozzleTip = null;
        }

        public BoardLocation getBoardLocation() {
            return boardLocation;
        }

        public Placement getPlacement() {
            return placement;
        }

        public void setStatus(Status status) {
            Object oldValue = this.status;
            this.status = status;
            firePropertyChange("status", oldValue, status);
        }

        public Status getStatus() {
            return status;
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

        public NozzleTip getNozzleTip() {
            return nozzleTip;
        }        
        @Override
        public String toString() {
            return placement.getId();
        }
    }
}
