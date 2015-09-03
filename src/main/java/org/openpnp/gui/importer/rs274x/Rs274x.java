package org.openpnp.gui.importer.rs274x;

import java.util.ArrayList;
import java.util.List;

import org.openpnp.model.Pad;

public class Rs274x {
    // apertures
    // flashes
    private List<Aperture> apertures = new ArrayList<>();
    private List<Flash> flashes = new ArrayList<>();
    
    public List<Pad> getPads() {
        return null;
    }
    
    public static class Aperture {
        
    }
    
    public static class StandardAperture extends Aperture {
        
    }
    
    public static class Flash {
        
    }
}
