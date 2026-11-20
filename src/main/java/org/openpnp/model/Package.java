/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openpnp.machine.reference.vision.AbstractPartSettingsHolder;
import org.openpnp.spi.NozzleTip;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Version;

public class Package extends AbstractPartSettingsHolder {
    @Version(revision=1.1)
    private double version;    
    
    @Attribute
    private String id;

    @Attribute(required = false)
    private String description;

    @Attribute(required = false)
    private String tapeSpecification;

    /**
     * The package's rotation in its carrier tape, in degrees, counter-clockwise positive, relative
     * to the package at 0 degrees. Measured in the EIA-481 tape frame: looking down on the cover
     * tape with the sprocket holes along the top and the tape feeding left to right. Null when not
     * known.
     */
    @Attribute(required = false)
    private Double tapeRotation;

    /**
     * The distance between parts in the carrier tape (component pitch P1 in EIA-481). Null when
     * not known.
     */
    @Element(required = false)
    private Length tapePitch;

    /**
     * The width of the carrier tape (W in EIA-481): 8, 12, 16, 24 mm and so on. Null when not
     * known.
     */
    @Element(required = false)
    private Length tapeWidth;

    @Attribute(required = false)
    private double pickVacuumLevel;

    @Attribute(required = false)
    private double placeBlowOffLevel;

    @Element(required = false)
    private Footprint footprint;
    
    @Element(required = false)
    private VisionCompositing visionCompositing;
    
    @ElementList(required = false)
    protected List<String> compatibleNozzleTipIds = new ArrayList<>();

    protected Set<NozzleTip> compatibleNozzleTips; 

    private Package() {
        this(null);
    }

    public Package(String id) {
        this.id = id;
        footprint = new Footprint();
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Warning: This should never be called once the Package is added to the configuration. It
     * should only be used when creating a new package.
     * 
     * @param id
     */
    public void setId(String id) {
        Object oldValue = this.id;
        this.id = id;
        firePropertyChange("id", oldValue, id);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        Object oldValue = this.description;
        this.description = description;
        firePropertyChange("description", oldValue, description);
    }

    public String getTapeSpecification() {
        return tapeSpecification;
    }

    public void setTapeSpecification(String tapeSpecification) {
        Object oldValue = this.tapeSpecification;
        this.tapeSpecification = tapeSpecification;
        firePropertyChange("tapeSpecification", oldValue, tapeSpecification);
    }

    public Double getTapeRotation() {
        return tapeRotation;
    }

    public void setTapeRotation(Double tapeRotation) {
        Object oldValue = this.tapeRotation;
        this.tapeRotation = tapeRotation;
        firePropertyChange("tapeRotation", oldValue, tapeRotation);
    }

    public Length getTapePitch() {
        return tapePitch;
    }

    public void setTapePitch(Length tapePitch) {
        Object oldValue = this.tapePitch;
        this.tapePitch = tapePitch;
        firePropertyChange("tapePitch", oldValue, tapePitch);
    }

    public Length getTapeWidth() {
        return tapeWidth;
    }

    public void setTapeWidth(Length tapeWidth) {
        Object oldValue = this.tapeWidth;
        this.tapeWidth = tapeWidth;
        firePropertyChange("tapeWidth", oldValue, tapeWidth);
    }

    public void setPlaceBlowOffLevel(double level) {
        Object oldValue = this.placeBlowOffLevel;
        this.placeBlowOffLevel = level;
        firePropertyChange("placeBlowOffLevel", oldValue, level);
    }

    public double getPlaceBlowOffLevel() {
        return placeBlowOffLevel;
    }

    public void setPickVacuumLevel(double level) {
        Object oldValue = this.pickVacuumLevel;
        this.pickVacuumLevel = level;
        firePropertyChange("pickVacuumLevel", oldValue, level);
    }

    public double getPickVacuumLevel() {
        return pickVacuumLevel;
    }

    public Footprint getFootprint() {
        return footprint;
    }

    public void setFootprint(Footprint footprint) {
        Object oldValue = this.footprint;
        this.footprint = footprint;
        firePropertyChange("footprint", oldValue, footprint);
    }

    public void fireFootprintChanged() {
        firePropertyChange("footprint", null, footprint); 
     }

    public VisionCompositing getVisionCompositing() {
        if (visionCompositing == null) {
            visionCompositing = new VisionCompositing();
        }
        return visionCompositing;
    }

    public void setVisionCompositing(VisionCompositing visionCompositing) {
        Object oldValue = this.visionCompositing;
        this.visionCompositing = visionCompositing;
        firePropertyChange("visionCompositing", oldValue, visionCompositing);
    }

    @Override
    public String toString() {
        return String.format("id %s", id);
    }
    
    protected void syncCompatibleNozzleTipIds() {
        compatibleNozzleTipIds.clear();
        for (NozzleTip nt : compatibleNozzleTips) {
            compatibleNozzleTipIds.add(nt.getId());
        }
    }
    
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

    public void addCompatibleNozzleTip(NozzleTip nt) {
        // Makes sure the structure has been initialized.
        getCompatibleNozzleTips();
        compatibleNozzleTips.add(nt);
        syncCompatibleNozzleTipIds();
        firePropertyChange("compatibleNozzleTips", null, getCompatibleNozzleTips());
    }

    public void removeCompatibleNozzleTip(NozzleTip nt) {
        // Makes sure the structure has been initialized.
        getCompatibleNozzleTips();
        compatibleNozzleTips.remove(nt);
        syncCompatibleNozzleTipIds();
        firePropertyChange("compatibleNozzleTips", null, getCompatibleNozzleTips());
    }
}
