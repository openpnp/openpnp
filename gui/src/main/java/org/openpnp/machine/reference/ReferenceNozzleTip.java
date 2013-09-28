package org.openpnp.machine.reference;

import java.util.ArrayList;
import java.util.List;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.base.AbstractNozzleTip;
import org.simpleframework.xml.Attribute;
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

}
