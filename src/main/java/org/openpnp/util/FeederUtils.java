package org.openpnp.util;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.pmw.tinylog.Logger;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Head;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Configuration;

public class FeederUtils {

    // Find an enabled feeder for a Part.
    //
    // If there is more than one feeder available, return a preferred feeder
    // if it is specified and still valid. Otherwise pick the closest to
    // the specified datum location, or closest to the default head.
    //
    public static Feeder findFeeder(Machine machine,Part part,Feeder preference,Location datum) throws Exception {
        return findClosest(machine,findFeeders(machine,part),preference,datum);
    }

    public static Feeder findClosest(Machine machine,List<Feeder> feeders,Feeder preference,Location datum) throws Exception {
        Head head = machine.getDefaultHead();
        if (datum==null) {
            datum = head.getDefaultCamera().getLocation();
        }

        if (feeders.isEmpty()) {
            return null;
        }

        if (feeders.contains(preference)) {
            return preference;
        }

        Feeder closestFeeder = null;
        double closestCost = 0;
        for(Feeder feeder : feeders) {
            double cost = datum.getLinearDistanceTo(feeder.getPickLocation());
            if (closestFeeder==null || cost<closestCost) {
                closestCost = cost;
                closestFeeder = feeder;
            }
        }
        return closestFeeder;
    }

    // Find all enabled feeders for a Part, then filter by priority
    public static List<Feeder> findFeeders(Machine machine,Part part) throws Exception {
        ArrayList<Feeder> feeders = new ArrayList<Feeder>();
        Feeder.Priority highestPriority = Feeder.Priority.Low;
        for (Feeder feeder : machine.getFeeders()) {
            if (feeder.getPart() == part && feeder.isEnabled()) {
                feeders.add(feeder);
                Feeder.Priority p = feeder.getPriority();
                if (p.ordinal()<highestPriority.ordinal()) {  // NB enum ordinal values are larger for low priorities
                    highestPriority = p;
                }
            }
        }
        ArrayList<Feeder> filteredFeeders = new ArrayList<Feeder>();
        for (Feeder feeder : feeders) {
            if (feeder.getPriority()==highestPriority) {
                filteredFeeders.add(feeder);
            }
        }

        return filteredFeeders;
    }

}
