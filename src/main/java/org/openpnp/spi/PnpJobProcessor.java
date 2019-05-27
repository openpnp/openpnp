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

        protected final BoardLocation boardLocation;
        protected final Placement placement;
        protected Status status = Status.Pending;
        protected Exception error;

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

        public void setError(Exception error) {
            Object oldValue = this.error;
            this.error = error;
            firePropertyChange("error", oldValue, error);
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
    
    public class PnpJobProcessorException extends Exception {
        private static final long serialVersionUID = 1L;
        
        private final Object source;
        
        public PnpJobProcessorException(Object source, Throwable throwable) {
            super(throwable.getMessage(), throwable);
            this.source = source;
        }
        
        public PnpJobProcessorException(Object source, String message) {
            super(message);
            this.source = source;
        }
        
        public Object getSource() {
            return source;
        }
    }
}
