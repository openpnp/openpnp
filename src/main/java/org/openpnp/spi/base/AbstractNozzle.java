package org.openpnp.spi.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import org.openpnp.gui.support.Icons;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

public abstract class AbstractNozzle extends AbstractModelObject implements Nozzle {
    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;
    
    @ElementList(required = false)
    protected List<String> compatibleNozzleTipIds = new ArrayList<>();

    protected Set<NozzleTip> compatibleNozzleTips; 

    protected Head head;
    
    protected Part part;
    
    public AbstractNozzle() {
        this.id = Configuration.createId("NOZ");
        this.name = getClass().getSimpleName();
    }
    
    @Override
    public String getId() {
        return id;
    }

    @Override
    public Head getHead() {
        return head;
    }

    @Override
    public void setHead(Head head) {
        this.head = head;
    }

    @Override
    public Location getCameraToolCalibratedOffset(Camera camera) {
        return new Location(camera.getUnitsPerPixel().getUnits());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        firePropertyChange("name", null, name);
    }
    
    @Override
    public Part getPart() {
        return part;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return Icons.captureTool;
    }
    
    protected void syncCompatibleNozzleTipIds() {
        compatibleNozzleTipIds.clear();
        for (NozzleTip nt : compatibleNozzleTips) {
            compatibleNozzleTipIds.add(nt.getId());
        }
    }

    @Override
    public Set<NozzleTip> getCompatibleNozzleTips() {
        if (compatibleNozzleTips == null) {
            compatibleNozzleTips = new HashSet<>();
            for (String nozzleTipId : compatibleNozzleTipIds) {
                NozzleTip nt = Configuration.get().getMachine().getNozzleTip(nozzleTipId);
                if (nt != null) {
                    compatibleNozzleTips.add(nt);
                }
            }
        }
        return Collections.unmodifiableSet(compatibleNozzleTips);
    }

    @Override
    public void addCompatibleNozzleTip(NozzleTip nt) {
        // Makes sure the structure has been initialized.
        getCompatibleNozzleTips();
        compatibleNozzleTips.add(nt);
        syncCompatibleNozzleTipIds();
        firePropertyChange("compatibleNozzleTips", null, getCompatibleNozzleTips());
    }

    @Override
    public void removeCompatibleNozzleTip(NozzleTip nt) {
        // Makes sure the structure has been initialized.
        getCompatibleNozzleTips();
        compatibleNozzleTips.remove(nt);
        syncCompatibleNozzleTipIds();
        firePropertyChange("compatibleNozzleTips", null, getCompatibleNozzleTips());
    }
}
