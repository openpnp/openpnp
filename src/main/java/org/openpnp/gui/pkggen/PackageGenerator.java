package org.openpnp.gui.pkggen;

import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Package;

public abstract class PackageGenerator extends AbstractModelObject {
    public abstract String[] getPropertyNames();
    
    public abstract Package getPackage();
}
