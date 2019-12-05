package org.openpnp.gui.pkggen.pkgs;

import java.io.StringWriter;

import org.openpnp.gui.pkggen.MinNomMaxField;
import org.openpnp.gui.pkggen.PackageGeneratorPackage;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Footprint.Pad;
import org.simpleframework.xml.Serializer;

public class QFN implements PackageGeneratorPackage {
    MinNomMaxField e = new MinNomMaxField("e");
    MinNomMaxField E = new MinNomMaxField("E");
    MinNomMaxField L = new MinNomMaxField("L");
    MinNomMaxField b = new MinNomMaxField("b");
    MinNomMaxField D = new MinNomMaxField("D");
    MinNomMaxField A = new MinNomMaxField("A");

    
    public MinNomMaxField[] getFields() {
        return new MinNomMaxField[] { e, E, L, b, D, A };
    }
    
    public org.openpnp.model.Package generate() {
        org.openpnp.model.Package pkg = new org.openpnp.model.Package("NEW");
        
        Footprint footprint = new Footprint();
        footprint.setUnits(LengthUnit.Millimeters);
        footprint.setBodyWidth(E.getNom());
        footprint.setBodyHeight(D.getNom());
        
        Pad pad;

        pad = new Pad();
        pad.setWidth(L.getNom());
        pad.setHeight(E.getNom());
        pad.setX(-(D.getNom() / 2) + (L.getNom() / 2));
        footprint.addPad(pad);
        
        pad = new Pad();
        pad.setWidth(L.getNom());
        pad.setHeight(E.getNom());
        pad.setX((D.getNom() / 2) - (L.getNom() / 2));
        footprint.addPad(pad);
        
        pkg.setFootprint(footprint);
        
        try {
            Serializer s = Configuration.createSerializer();
            StringWriter w = new StringWriter();
            s.write(pkg, w);
            System.out.println(w.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return pkg;
    }
}
