package org.openpnp.machine.reference;

import java.util.ArrayList;
import java.util.List;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.base.AbstractNozzleTip;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceNozzleTip extends AbstractNozzleTip {
    private final static Logger logger = LoggerFactory
            .getLogger(ReferenceNozzleTip.class);

    @ElementList(required = false, entry = "id")
    private List<String> compatiblePackageIds = new ArrayList<String>();
    
    @Attribute(required = false)
    private boolean allowIncompatiblePackages;
    
    @Element(required = false)
    private Location changerStartLocation = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    private Location changerMidLocation = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    private Location changerEndLocation = new Location(LengthUnit.Millimeters);

    private List<org.openpnp.model.Package> compatiblePackages = new ArrayList<org.openpnp.model.Package>();
    
    public ReferenceNozzleTip() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration)
                    throws Exception {
                for (String id : compatiblePackageIds) {
                    org.openpnp.model.Package pkg = configuration.getPackage(id);
                    if (pkg == null) {
                        throw new Exception("Package " + id
                                + " not found for ReferenceNozzleTip.");
                    }
                    compatiblePackages.add(pkg);
                }
            }
        });
    }

    @Override
    public boolean canHandle(Part part) {
        boolean result = allowIncompatiblePackages
                || compatiblePackages.contains(part.getPackage());
		logger.debug("{}.canHandle({}) => {}", new Object[]{getId(), part.getId(), result});
		return result;
	}

	@Override
	public String toString() {
		return getId();
	}

	@Override
	public Wizard getConfigurationWizard() {
		// TODO Auto-generated method stub
		return null;
	}

    public Location getChangerStartLocation() {
        return changerStartLocation;
    }

    public void setChangerStartLocation(Location changerStartLocation) {
        this.changerStartLocation = changerStartLocation;
    }

    public Location getChangerMidLocation() {
        return changerMidLocation;
    }

    public void setChangerMidLocation(Location changerMidLocation) {
        this.changerMidLocation = changerMidLocation;
    }

    public Location getChangerEndLocation() {
        return changerEndLocation;
    }

    public void setChangerEndLocation(Location changerEndLocation) {
        this.changerEndLocation = changerEndLocation;
    }
}
