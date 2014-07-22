package org.openpnp.machine.reference;

import java.util.HashSet;
import java.util.Set;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleTipConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.PropertySheetConfigurable;
import org.openpnp.spi.PropertySheetConfigurable.PropertySheet;
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
    private Set<String> compatiblePackageIds = new HashSet<String>();
    
    @Attribute(required = false)
    private boolean allowIncompatiblePackages;
    
    @Element(required = false)
    private Location changerStartLocation = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    private Location changerMidLocation = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    private Location changerEndLocation = new Location(LengthUnit.Millimeters);

    private Set<org.openpnp.model.Package> compatiblePackages = new HashSet<org.openpnp.model.Package>();
    
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
    
	public Set<org.openpnp.model.Package> getCompatiblePackages() {
        return new HashSet<org.openpnp.model.Package>(compatiblePackages);
    }

    public void setCompatiblePackages(
            Set<org.openpnp.model.Package> compatiblePackages) {
        this.compatiblePackages.clear();
        this.compatiblePackages.addAll(compatiblePackages);
        compatiblePackageIds.clear();
        for (org.openpnp.model.Package pkg : compatiblePackages) {
            compatiblePackageIds.add(pkg.getId());
        }
    }

    @Override
	public String toString() {
		return getId();
	}

	@Override
	public Wizard getConfigurationWizard() {
	    return new ReferenceNozzleTipConfigurationWizard(this);
	}

    @Override
    public String getPropertySheetConfigurableTitle() {
        return getClass().getSimpleName() + " " + getId();
    }

    @Override
    public PropertySheetConfigurable[] getPropertySheetConfigurableChildren() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard())
        };
    }

    public boolean isAllowIncompatiblePackages() {
        return allowIncompatiblePackages;
    }

    public void setAllowIncompatiblePackages(boolean allowIncompatiblePackages) {
        this.allowIncompatiblePackages = allowIncompatiblePackages;
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
