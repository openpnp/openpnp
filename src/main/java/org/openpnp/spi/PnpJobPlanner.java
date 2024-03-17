package org.openpnp.spi;

import java.util.List;

import org.openpnp.spi.PnpJobProcessor.JobPlacement;
import org.openpnp.model.Location;

public interface PnpJobPlanner {
    public static class PlannedPlacement {
        public final JobPlacement jobPlacement;
        public final Nozzle nozzle;
        public final NozzleTip nozzleTip;
        public Feeder feeder;
        public PartAlignment.PartAlignmentOffset alignmentOffsets;
        
        // the following locations are used for optimization purposes and may not be accurate or complete.
        private Location pickLocation;	// (preliminary) location the pick will take place - as feed/pick might not work, this location is preliminary and optimization using it my fail.
        private Location alignLocation; // location the alignment will take place. - may be null if alignment for this part is disabled
        private Location placeLocation; // location the part will be place to

        public enum LocationType {
            PICK("pick"), ALIGN("alignment"), PLACE("place");   // location type, used to return one of [pick|align|place]Location
            
            private final String name;
            
            LocationType(String name) {
                this.name = name;
            }
            
            @Override
            public String toString() {
                return name;
            }
        }
        
        // return the location of given type
        public Location getLocation(LocationType type) {
            Location l = null;
            switch (type)
            {
                case PICK:
                    l = pickLocation;
                    break;
                    
                case ALIGN:
                    l = alignLocation;
                    break;
                    
                case PLACE:
                    l = placeLocation;
                    break;
            }
            
            return l;
        }

        // set a specific location type
        public void setLocation(Location l, LocationType type) {
            switch (type)
            {
                case PICK:
                    pickLocation = l;
                    break;
                    
                case ALIGN:
                    alignLocation = l;
                    break;
                    
                case PLACE:
                    placeLocation = l;
                    break;
            }
        }
        
        public PlannedPlacement(Nozzle nozzle, NozzleTip nozzleTip, JobPlacement jobPlacement) {
            this.nozzle = nozzle;
            this.nozzleTip = nozzleTip;
            this.jobPlacement = jobPlacement;
        }

        @Override
        public String toString() {
            return String.format("%s (%s) -> %s", nozzle.getName(), nozzleTip.getName(), jobPlacement);
        }
    }

    
    public List<PlannedPlacement> plan(Head head, List<JobPlacement> placements);
}
