package org.openpnp.gui.pkggen;

import org.openpnp.model.Package;

public interface PackageGeneratorPackage {
    public MinNomMaxField[] getFields();
    
    public Package generate();
}
