package org.openpnp.machine.reference;

import java.util.ArrayList;
import java.util.List;

import org.openpnp.RequiresConfigurationResolution;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.base.AbstractNozzleTip;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceNozzleTip extends AbstractNozzleTip implements
        RequiresConfigurationResolution {
    private final static Logger logger = LoggerFactory
            .getLogger(ReferenceNozzleTip.class);

    @ElementList(required = false, entry = "id")
    private List<String> compatiblePackageIds = new ArrayList<String>();
    @Attribute(required = false)
    private boolean allowIncompatiblePackages;

    private List<org.openpnp.model.Package> compatiblePackages = new ArrayList<org.openpnp.model.Package>();

    @Override
    public void resolve(Configuration configuration) throws Exception {
        for (String id : compatiblePackageIds) {
            org.openpnp.model.Package pkg = configuration.getPackage(id);
            if (pkg == null) {
                throw new Exception("Package " + id + " not found for ReferenceNozzleTip.");
            }
            compatiblePackages.add(pkg);
        }
    }

    @Override
    public boolean canHandle(Part part) {
        return allowIncompatiblePackages
                || compatiblePackages.contains(part.getPackage());
    }
}
