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
        private Status status = Status.Pending;
        private Exception error;

        public JobPlacement(BoardLocation boardLocation, Placement placement) {
            this.boardLocation = boardLocation;
            this.placement = placement;
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
