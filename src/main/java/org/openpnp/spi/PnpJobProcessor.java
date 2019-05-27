package org.openpnp.spi;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Placement;

public interface PnpJobProcessor extends JobProcessor {
    public static class JobPlacement {
        public enum Status {
            Pending,
            Processing,
            Errored,
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
