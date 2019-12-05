package org.openpnp.gui.pkggen.pkgs;

import org.openpnp.gui.pkggen.MinNomMaxField;
import org.openpnp.gui.pkggen.PackageGeneratorPackage;
import org.openpnp.model.Footprint;
import org.openpnp.model.Footprint.Pad;
import org.openpnp.model.LengthUnit;

public class Chip implements PackageGeneratorPackage {
    MinNomMaxField D = new MinNomMaxField("D");
    MinNomMaxField E = new MinNomMaxField("E");
    MinNomMaxField L = new MinNomMaxField("L");
    MinNomMaxField L1 = new MinNomMaxField("L1");
    
    public MinNomMaxField[] getFields() {
        return new MinNomMaxField[] { D, E, L, L1 };
    }
    
    public org.openpnp.model.Package generate() {
        Footprint footprint = new Footprint();
        footprint.setUnits(LengthUnit.Millimeters);
        footprint.setBodyWidth(D.getNom());
        footprint.setBodyHeight(E.getNom());
        
        Pad pad;

        pad = new Pad();
        pad.setName("1");
        pad.setWidth(L.getNom());
        pad.setHeight(E.getNom());
        pad.setX(-(D.getNom() / 2) + (L.getNom() / 2));
        footprint.addPad(pad);
        
        pad = new Pad();
        pad.setName("2");
        pad.setWidth(L.getNom());
        pad.setHeight(E.getNom());
        pad.setX((D.getNom() / 2) - (L.getNom() / 2));
        footprint.addPad(pad);
        
        org.openpnp.model.Package pkg = new org.openpnp.model.Package("NEW");
        pkg.setFootprint(footprint);
        
        return pkg;
    }
}
