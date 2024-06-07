package org.openpnp.gui.importer.diptrace.csv;

import org.openpnp.model.*;
import org.openpnp.model.Package;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DipTraceCSVParser {
    public List<Placement> parse(File file, boolean createMissingParts)
            throws Exception {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        ArrayList<Placement> placements = new ArrayList<>();
        String line;
        int lineCount = 0;

        //
        // Default format for DIPTRACE pick and place export is
        // RefDes,Name,X (mm),Y (mm),Side,Rotate,Value
        // C1,C0603,8.6,7.2,Top,0,1nF
        // C2,C0402,10.81,22.99,Top,180,0.1uF/16V
        // <etc>

        while ((line = reader.readLine()) != null) {

        	// Skip first line as it's always header
        	if (lineCount++ == 0 || line.length() == 0)  {
                continue;
            }
            line = line.trim();

            String[] tokens = line.split(","); //$NON-NLS-1$

            String placementId = tokens[0];  							// RefDes in Diptrace export
            String partValue = tokens[6];    							// Value in Diptrace export
            String pkgName = tokens[1];      							// Name in Diptrace export
            double placementX = Double.parseDouble(tokens[2]);   		// X (mm) in Diptrace export
            double placementY = Double.parseDouble(tokens[3]);   		// Y (mm) in Diptrace export
            double placementRotation = Double.parseDouble(tokens[5]); 	// Rotate in Diptrace export
            String placementLayer = tokens[4];    						// Side in Diptrace export

            Placement placement = new Placement(placementId);
            placement.setLocation(new Location(LengthUnit.Millimeters, placementX, placementY, 0,
                    placementRotation));
            Configuration cfg = Configuration.get();
            if (cfg != null && createMissingParts) {
                String partId = pkgName + "-" + partValue; //$NON-NLS-1$
                Part part = cfg.getPart(partId);
                if (part == null) {
                    part = new Part(partId);
                    org.openpnp.model.Package pkg = cfg.getPackage(pkgName);
                    if (pkg == null) {
                        pkg = new Package(pkgName);
                        cfg.addPackage(pkg);
                    }
                    part.setPackage(pkg);

                    cfg.addPart(part);
                }
                placement.setPart(part);

            }

            placement.setSide(placementLayer.charAt(0) == 'T' ? Abstract2DLocatable.Side.Top : Abstract2DLocatable.Side.Bottom);
            placements.add(placement);
        }
        reader.close();
        return placements;
    }
}
