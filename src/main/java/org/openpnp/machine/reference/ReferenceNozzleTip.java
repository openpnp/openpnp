package org.openpnp.machine.reference;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleTipConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractNozzleTip;
import org.openpnp.util.UiUtils;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceNozzleTip extends AbstractNozzleTip {
    private final static Logger logger = LoggerFactory
            .getLogger(ReferenceNozzleTip.class);

    @ElementList(required = false, entry = "id")
    private Set<String> compatiblePackageIds = new HashSet<>();
    
    @Attribute(required = false)
    private boolean allowIncompatiblePackages;
    
    @Element(required = false)
    private Location changerStartLocation = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    private Location changerMidLocation = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    private Location changerEndLocation = new Location(LengthUnit.Millimeters);

    private Set<org.openpnp.model.Package> compatiblePackages = new HashSet<>();
    
    public ReferenceNozzleTip() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration)
                    throws Exception {
                for (String id : compatiblePackageIds) {
                    org.openpnp.model.Package pkg = configuration.getPackage(id);
                    if (pkg == null) {
                        continue;
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
		logger.debug("{}.canHandle({}) => {}", new Object[]{getName(), part.getId(), result});
		return result;
	}
    
	public Set<org.openpnp.model.Package> getCompatiblePackages() {
        return new HashSet<>(compatiblePackages);
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
		return getName();
	}

	@Override
	public Wizard getConfigurationWizard() {
	    return new ReferenceNozzleTipConfigurationWizard(this);
	}

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] {
                unloadAction,
                loadAction
        };
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
    
    private Nozzle getParentNozzle() {
        for (Head head : Configuration.get().getMachine().getHeads()) {
            for (Nozzle nozzle : head.getNozzles()) {
                for (NozzleTip nozzleTip : nozzle.getNozzleTips()) {
                    if (nozzleTip == this) {
                        return nozzle;
                    }
                }
            }
        }
        return null;
    }
    
    public Action loadAction = new AbstractAction("Load") {
        {
            putValue(SMALL_ICON, Icons.load);
            putValue(NAME, "Load");
            putValue(SHORT_DESCRIPTION,
                    "Load the currently selected nozzle tip.");
        }
        
        @Override
        public void actionPerformed(final ActionEvent arg0) {
        	UiUtils.submitUiMachineTask(() -> {
                getParentNozzle().loadNozzleTip(ReferenceNozzleTip.this);
        	});
        }
    };
    
    public Action unloadAction = new AbstractAction("Unoad") {
        {
            putValue(SMALL_ICON, Icons.unload);
            putValue(NAME, "Unload");
            putValue(SHORT_DESCRIPTION,
                    "Unoad the currently loaded nozzle tip.");
        }
        
        @Override
        public void actionPerformed(final ActionEvent arg0) {
        	UiUtils.submitUiMachineTask(() -> {
                getParentNozzle().unloadNozzleTip();
        	});
        }
    };
}
